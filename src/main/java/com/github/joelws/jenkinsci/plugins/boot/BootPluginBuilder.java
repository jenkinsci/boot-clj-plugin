package com.github.joelws.jenkinsci.plugins.boot;


import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Boot plugin {@link Builder}.
 * This plugin allows projects to be built using Boot.
 *
 * @author Joel Whittaker-Smith
 */

public class BootPluginBuilder extends Builder {

    private static final Logger LOGGER = Logger.getLogger(BootPluginBuilder.class.getName());

    private static final String BOOT_PATH_ERROR = "Can't determine Boot path!";

    private static final String INVALID_TASK_ERROR = "Invalid task";

    private static final String COMMAND_EXECUTION_ERROR = "Command could not be executed";

    private static int SUCCESSFUL_EXIT_STATUS = 0;

    private static final String BOOT_JVM_OPTIONS = "BOOT_JVM_OPTIONS";

    private final String tasks;

    private final String jvmOpts;

    @DataBoundConstructor
    public BootPluginBuilder(String tasks, String jvmOpts) {
        this.tasks = tasks;
        this.jvmOpts = jvmOpts;
    }


    public String getTasks() { return tasks; }

    public String getJvmOpts() { return jvmOpts; }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        EnvVars env;
        FilePath workDirectory = build.getModuleRoot();
        if (tasks == null || tasks.trim().isEmpty()) {
            listener.fatalError(INVALID_TASK_ERROR);
            build.setResult(Result.FAILURE);
            return false;
        }


        try {
            ArgumentListBuilder bootCommand = generateBootCommand(launcher);
            String[] bootCMD = bootCommand.toCommandArray();
            env = build.getEnvironment(listener);

            if (launcher.isUnix()) {
                addJVMOpts(env);
            }

            int exitVal = launcher.launch().cmds(bootCMD).envs(env).stdout(listener).pwd(workDirectory).join();
            if (exitVal == SUCCESSFUL_EXIT_STATUS) {
                build.setResult(Result.SUCCESS);
                return true;
            } else {
                build.setResult(Result.FAILURE);
                return false;
            }

        } catch (IllegalArgumentException e) {
            listener.fatalError(COMMAND_EXECUTION_ERROR + ": " + e.getMessage());
            LOGGER.log(Level.SEVERE, COMMAND_EXECUTION_ERROR + ": " + e.getMessage());
            build.setResult(Result.FAILURE);
            return false;

        } catch (IOException e) {
            listener.fatalError(COMMAND_EXECUTION_ERROR + ": " + e.getMessage());
            LOGGER.log(Level.SEVERE, COMMAND_EXECUTION_ERROR + ": " + e.getMessage());
            build.setResult(Result.FAILURE);
            return false;
        } catch (InterruptedException e) {
            listener.fatalError(COMMAND_EXECUTION_ERROR + ": " + e.getMessage());
            LOGGER.log(Level.SEVERE, "ABORTED: " + e.getMessage());
            build.setResult(Result.ABORTED);
            return false;
        }

    }

    private EnvVars addJVMOpts(EnvVars envVars) {
        if (jvmOpts != null && !jvmOpts.trim().isEmpty()) {
            StringBuilder bootJVMOpts = new StringBuilder();

            String[] opts = jvmOpts.split("\\s+");
            for (String opt : opts) {
                bootJVMOpts.append(opt);
                bootJVMOpts.append(" ");
            }

            bootJVMOpts.setLength(bootJVMOpts.length() - 1);
            envVars.put(BOOT_JVM_OPTIONS, bootJVMOpts.toString().trim());
        }
        return envVars;
    }


    private ArgumentListBuilder generateBootCommand(Launcher launcher) {
        DescriptorImpl descriptor = (DescriptorImpl) getDescriptor();
        ArgumentListBuilder args = new ArgumentListBuilder();
        String bootScriptPath = descriptor.getBootFilePath();

        if (bootScriptPath == null || bootScriptPath.trim().isEmpty()) {
            throw new IllegalArgumentException(BOOT_PATH_ERROR);

        }
        if (!launcher.isUnix()) {
            args.add("cmd.exe", "/C");
        }
        args.add(bootScriptPath);
        String[] bootTasks = tasks.trim().split("\\s+");
        for (String task : bootTasks) {
            args.add(task);
        }
        return args;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private static final String DISPLAY_NAME = "Build project using boot";

        private static final String BOOT_FILENAME = "boot";

        private static final String FORM_VALIDATION_ERROR = "Please provide a valid boot path";

        private String bootFilePath;

        public DescriptorImpl() {
            load();
        }

        public String getBootFilePath() {
            return bootFilePath;
        }

        /**
         * On the fly validation for bootFilePath field via getter
         */
        public FormValidation doCheckBootFilePath(@QueryParameter String value)
                throws IOException, ServletException {
            if ((value.length() > 0) && (value.endsWith(BOOT_FILENAME))) {
                return FormValidation.ok();
            }
            return FormValidation.error(FORM_VALIDATION_ERROR);

        }

        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            bootFilePath = formData.getString("bootFilePath");
            save();
            return super.configure(req, formData);
        }
    }


}
