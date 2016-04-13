package com.github.joelws.jenkinsci.plugins.boot;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import static org.junit.Assert.assertEquals;


/**
 * Testing plugin configuration in project persists on save
 * @author Joel Whittaker-Smith
 */
public class BootPluginBuilderConfigSubmitTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private WebClient webClient;

    @Before
    public void setUp() throws Exception {
        webClient = j.createWebClient();
    }

    @Test
    public void testConfigSubmit() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        BootPluginBuilder bootPluginBuilder = new BootPluginBuilder("help", "-client");
        project.getBuildersList().add(bootPluginBuilder);
        HtmlForm form = webClient.goTo(project.getUrl() + "/configure").getFormByName("config");
        HtmlTextInput bootTaskInputBox = form.getInputByName("_.tasks");
        assertEquals(bootTaskInputBox.getValueAttribute(), "help");
        BootPluginBuilder persisted = project.getBuildersList().get(BootPluginBuilder.class);
        assertEquals(persisted.getTasks(), "help");
        assertEquals(persisted.getJvmOpts(), "-client");

    }
}
