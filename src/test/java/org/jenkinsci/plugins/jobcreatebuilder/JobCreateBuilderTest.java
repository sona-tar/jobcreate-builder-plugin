package org.jenkinsci.plugins.jobcreatebuilder;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.util.OneShotEvent;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class JobCreateBuilderTest {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void testPerform() throws Exception {
        // create a job. Job name is pre-job01
        {
            // Jenkins global config
            String newJobNamePrefix = "pre-";
            // Job local config
            String newJobName = "job01";

            // setup
            FreeStyleProject createBuilderProject = createJobCreateBuilderProject("create-builder-project01", newJobNamePrefix, newJobName);

            // start test
            Build build = createBuilderProject.scheduleBuild2(0).get();
            Result result = getResult(build);

            // check result
            assertThat("Check pre-job01 is success", result, is(Result.SUCCESS));

            FreeStyleProject newJob = (FreeStyleProject) jenkinsRule.getInstance().getItem(newJobNamePrefix + newJobName);
            assertThat("Check pre-job01 is created", newJob.getDisplayName(), is(newJobNamePrefix + newJobName));

            // tear down
            createBuilderProject.delete();
            newJob.delete();
        }

        // duplicate jobs. Job name are pre-job01
        {
            // Jenkins global config
            String newJobNamePrefix = "pre-";
            // Job local config
            String newJobName = "job01";

            // setup
            FreeStyleProject createBuilderProject = createJobCreateBuilderProject("create-builder-project01", newJobNamePrefix, newJobName);

            // start test
            Build build = createBuilderProject.scheduleBuild2(0).get();
            Result result = getResult(build);
            assertThat("Check pre-job01 is success", result, is(Result.SUCCESS));

            Build build2 = createBuilderProject.scheduleBuild2(0).get();
            Result result2 = getResult(build2);

            createBuilderProject.delete();

            // check result
            assertThat("Check pre-job01 is success", result2, is(Result.FAILURE));
            assertThat("Check job counts", jenkinsRule.getInstance().getAllItems().size(), is(1));

            FreeStyleProject newJob = (FreeStyleProject) jenkinsRule.getInstance().getItem(newJobNamePrefix + newJobName);
            assertThat("Check pre-job01 is created", newJob.getDisplayName(), is(newJobNamePrefix + newJobName));

            // tear down
            newJob.delete();
        }
    }

    private Result getResult(Build build) throws InterruptedException {
        while(build.isBuilding()) {
            Thread.sleep(10);
        }
        return build.getResult();
    }

    private FreeStyleProject createJobCreateBuilderProject(String builderName, String jobPrefix, String jobName) throws IOException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject(builderName);
        JobCreateBuilder job = new JobCreateBuilder(jobName);
        job.getDescriptor().setPrefix(jobPrefix);
        project.getBuildersList().add(job);
        return project;
    }

    @Test
    public void testDescriptorDoCheckTarget() {
        JobCreateBuilder.DescriptorImpl descriptor = getDescriptor();

        String stringJobName = "Sample-Job1";
        String regexpJobName = "Sample-Job*";
        String envJobName = "${TARGET_JOB}";
        String emptyJobName = "";

        {
            assertThat(
                    "Normal String",
                    descriptor.doCheckTarget(stringJobName).kind, is(FormValidation.Kind.OK)
            );

            assertThat(
                    "Regexp String",
                    descriptor.doCheckTarget(regexpJobName).kind, is(FormValidation.Kind.OK)
            );

            assertThat(
                    "Env String",
                    descriptor.doCheckTarget(envJobName).kind, is(FormValidation.Kind.OK)
            );

            assertThat(
                    "Empty String",
                    descriptor.doCheckTarget(emptyJobName).kind, is(FormValidation.Kind.ERROR)
            );
        }
    }

    private JobCreateBuilder.DescriptorImpl getDescriptor() {
        return (JobCreateBuilder.DescriptorImpl)new JobCreateBuilder("").getDescriptor();
    }
}