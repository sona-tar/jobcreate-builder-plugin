package org.jenkinsci.plugins.jobcreatebuilder;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;


public class JobCreateBuilder extends Builder {

    private final String target;

    @DataBoundConstructor
    public JobCreateBuilder(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        String newJobName = getCreateJobNameFromParam(build.getEnvironment(listener));
        if (newJobName.isEmpty()) {
            listener.getLogger().println("Error : " + Messages.JobCreateBuilder_JobName_Empty());
            return false;
        }
        try {
            Jenkins.getInstance().createProject(FreeStyleProject.class, getDescriptor().getPrefix() + newJobName);
        }catch (IllegalArgumentException e) {
            listener.getLogger().println("Error : " + e.getMessage());
            return false;
        }

        listener.getLogger().println(Messages.JobCreateBuilder_Success() + " name : " + newJobName);

        return true;
    }

    private String getCreateJobNameFromParam(EnvVars env){
        if (StringUtils.isBlank(target)) {
            return "";
        }

        // Expand the variable expressions in job names.
        String input = env.expand(target);
        if(StringUtils.isBlank(input)) {
            return "";
        }
        return input;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }


    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private String jobNamePrefix;

        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckTarget(@QueryParameter String value) {
            if (value.length() == 0)
                return FormValidation.error(Messages.JobCreateBuilder_JobName_Empty());
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        public boolean configure(StaplerRequest req, JSONObject formData)throws FormException {
            jobNamePrefix = formData.getString("prefix");
            save();
            return super.configure(req, formData);
        }

        public String getPrefix() {
            return jobNamePrefix;
        }
        public void setPrefix(String prefix) {
            jobNamePrefix = prefix;
        }

        public String getDisplayName() {
            return Messages.JobCreateBuilder_DisplayName();
        }
    }
}

