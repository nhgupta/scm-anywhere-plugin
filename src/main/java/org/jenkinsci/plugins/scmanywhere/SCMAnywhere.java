package org.jenkinsci.plugins.scmanywhere;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.Launcher.LocalLauncher;
import hudson.model.AbstractBuild;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.json.JSONObject;
import hudson.ProxyConfiguration;
import java.net.InetSocketAddress;
import java.net.Proxy;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

public class SCMAnywhere extends SCM implements Serializable {

    /**
     * Source repository URL from which we pull.
     */
    @CheckForNull
    private String server;
    @CheckForNull
    private String port;
    @CheckForNull
    private String username;
    @CheckForNull
    private String password;
    @CheckForNull
    private String teamproject;
    @CheckForNull
    private String folder;
    private static final long serialVersionUID = 1L;

    @DataBoundConstructor
    public SCMAnywhere(String server, String port, String username, String password, String teamproject, String folder) {
        this.setPort(port);
        this.setUsername(username);
        this.setPassword(password);
        this.setTeamproject(teamproject);
        this.setFolder(folder);
        this.setServer(server);
    }

    @DataBoundSetter
    public void setTeamproject(@CheckForNull final String teamproject) {
        this.teamproject = Util.fixEmptyAndTrim(teamproject);
    }

    @DataBoundSetter
    public void setFolder(@CheckForNull final String folder) {
        this.folder = Util.fixEmptyAndTrim(folder);
    }

    /**
     * Set the IPAdress from where project need to be extracted.
     *
     */
    @DataBoundSetter
    public void setServer(@CheckForNull final String server) {
        this.server = Util.fixEmptyAndTrim(server);
    }

    @DataBoundSetter
    public void setUsername(@CheckForNull final String username) {
        this.username = Util.fixEmptyAndTrim(username);
    }

    @DataBoundSetter
    public void setPassword(String password) {
        this.password = Util.fixEmptyAndTrim(password);
    }

    @DataBoundSetter
    public void setPort(String port) {
        this.port = Util.fixEmptyAndTrim(port);
    }

    /**
     * Gets the teamproject from where project need to be extracted.
     *
     * @return
     */
    @Exported
    public String getTeamproject() {
        return this.teamproject;
    }

    @Exported
    public String getFolder() {
        return this.folder;
    }

    @Exported
    public String getServer() {
        return this.server;
    }

    @Exported
    public String getUsername() {
        return this.username;
    }

    @Exported
    public String getPassword() {
        return this.password;
    }

    @Exported
    public String getPort() {
        return this.port;
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener)
            throws IOException, InterruptedException {
        PrintStream output = listener.getLogger();
        output.println("Getting local revision...");
        SCMRevisionState lastState = build.getAction(SCMAnyWhereRevisionState.class);
        output.println(lastState);
        return lastState;
    }

    private SCMAnyWhereRevisionState getLastState(Run<?, ?> build) {
    	SCMAnyWhereRevisionState lastState = null;
        while (build != null) {
            lastState = build.getAction(SCMAnyWhereRevisionState.class);
            if (lastState != null) {
            	break;
            }
            build = build.getPreviousBuild();
        }
        return lastState;
    }

    private ArgumentListBuilder getChangeSet(InetSocketAddress proxy, boolean detail)
            throws IOException, InterruptedException {
        ArgumentListBuilder arg = new ArgumentListBuilder();
        arg.add(getDescriptor().getScmExe());
        arg.add("GetFolderHistory");

        if (this.server != null && !"".equals(this.server)) {
            arg.add("-server");
            arg.add(getServer());
        }
        if (this.port != null && !"".equals(this.port)) {
            arg.add("-port");
            arg.add(getPort());
        }

        if (this.username != null && !"".equals(this.username)) {
            arg.add("-username");
            arg.add(getUsername());
        }

        if (this.password != null && !"".equals(this.password)) {
            arg.add("-pwd");
            arg.add(getPassword());
        }

        if (this.teamproject != null && !"".equals(this.teamproject)) {
            arg.add("-teamproject");
            arg.add(getTeamproject());
        }

        if (this.folder != null  && !"".equals(this.folder)) {
            arg.add("-folder");
            arg.add(getFolder());
        }

        if (proxy != null) {
            arg.add("-ptype");
            arg.add("\"http\"");
            arg.add("-pserver");
            arg.add(proxy.getHostName());
            arg.add("-pport");
            arg.add(proxy.getPort());
        }

        if (detail) {
            arg.add("-v");
        }
        return arg;
    }

    @SuppressWarnings("deprecation")
    private ArgumentListBuilder getLatestProject(FilePath workspace, InetSocketAddress proxy)
            throws IOException, InterruptedException {

        ArgumentListBuilder arg = new ArgumentListBuilder();
        arg.add(getDescriptor().getScmExe());
        arg.add("GetLatestFolder");

        if (this.server != null && !"".equals(this.server)) {
            arg.add("-server");
            arg.add(getServer());
        }
        if (this.port != null && !"".equals(this.port)) {
            arg.add("-port");
            arg.add(getPort());
        }

        if (this.username != null && !"".equals(this.username)) {
            arg.add("-username");
            arg.add(getUsername());
        }

        if (this.password != null && !"".equals(this.password)) {
            arg.add("-pwd");
            arg.add(getPassword());
        }

        if (this.teamproject != null && !"".equals(this.teamproject)) {
            arg.add("-teamproject");
            arg.add(getTeamproject());
        }

        if (this.folder != null && !"".equals(this.folder)) {
            arg.add("-folder");
            arg.add(getFolder());
        }

        arg.add("-workdir");
        arg.add(workspace.toString().replace("\\", "/"));

        if (proxy != null) {
            arg.add("-ptype");
            arg.add("\"http\"");
            arg.add("-pserver");
            arg.add(proxy.getHostName());
            arg.add("-pport");
            arg.add(proxy.getPort());
        }
        arg.add("-replace");
        arg.add("-r");
        return arg;
    }

    @Override
    public PollingResult compareRemoteRevisionWith(@Nonnull final Job<?, ?> job, @Nullable Launcher launcher,
            @Nullable final FilePath workspace, @Nonnull final TaskListener listener,
            @Nonnull final SCMRevisionState baseline) throws IOException, InterruptedException {

        PrintStream output = listener.getLogger();
        final Jenkins jenkins = Jenkins.getInstance();

        launcher = jenkins.createLauncher(listener);
        SCMAnyWhereRevisionState myBaseline = (SCMAnyWhereRevisionState) baseline;

        final Run<?, ?> lastRun = job.getLastBuild();
        output.printf(lastRun.getTimestampString() + lastRun.getDisplayName() + "\n");

        /* Get the proxy details setting from the Jenkins manage configuration */
        ProxyConfiguration proxyConfig = jenkins.proxy;
        InetSocketAddress proxy = null;
        if (proxyConfig != null) {
            Proxy myproxy = proxyConfig.createProxy(server);
            if (myproxy != null && myproxy.type() == Proxy.Type.HTTP) {
                proxy = (InetSocketAddress) myproxy.address();
            }
        }

        if (myBaseline == null) {
            // Probably the first build, or possibly an aborted build
            myBaseline = getLastState(lastRun);
            if (myBaseline == null)
                output.println("No Baseline");
            return PollingResult.BUILD_NOW;
        }

        output.println("Getting current remote revision...  Test with Jenkins");
        ArgumentListBuilder arg = getChangeSet(proxy, true);

        int highestChangeSetID = 0;
        String DateTime = "";
        try {
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            if (launcher.launch().cmds(arg).stdout(byteArray).join() != 0)
                listener.error("\nError in argument builder\n");
            else
            	output.print("Succesful pooling\n");
            output.print(byteArray);

            String loggingData = byteArray.toString();
            String[] splitLines = loggingData.split("\n");

            if (splitLines.length != 0) {
                highestChangeSetID = getChangeSetID(splitLines);
                DateTime = getRevisionDateTime(splitLines);
                output.print("\nFirst change set : " + highestChangeSetID + " : " + DateTime + "\n");
            }
        } catch (IOException e) {
            listener.error("Failed to run scm clean-tree\n");
        }

        final SCMAnyWhereRevisionState previousState = getLastState(lastRun);
        output.print("Previous State :" + previousState.getRevNo() + " " + previousState.getRevDateTime());
        output.print("Current State :" + highestChangeSetID + " " + DateTime);
        // comparing the revision state detail with the previous build
        if (!String.valueOf(highestChangeSetID).equals(previousState.getRevNo())
                && !String.valueOf(DateTime).equals(previousState.getRevDateTime())) {
        	output.print("build now after comparing the revisions");
            return PollingResult.BUILD_NOW;
        }
        return PollingResult.NO_CHANGES;
    }

    /* find the latest change set ID for the project from the logging */
    private int getChangeSetID(String[] splitLines) {
        int changeSetID = 0;
        if (splitLines.length > 1) {
            for (String line : splitLines) {
                String[] split = line.split("\\s+");
                if (split[0].matches("-?\\d+(\\.\\d+)?")) {
                    changeSetID = Integer.parseInt(split[0]);
                    break;
                }
            }
        }
        return changeSetID;
    }

    /* find the latest checkout date and time from the logging */
    private String getRevisionDateTime(String[] splitLines) {
        String dateTime = "";
        if (splitLines.length > 1) {
            for (String line : splitLines) {
                String[] split = line.split("\\s+");
                if (split[0].matches("-?\\d+(\\.\\d+)?")) {
                    dateTime = split[2] + " " + split[3];
                    break;
                }
            }
        }
        return dateTime;
    }

    @Override
    public void checkout(Run<?, ?> build, Launcher launcher, FilePath workspace, TaskListener listener,
            File changelogFile, SCMRevisionState baseline) throws IOException, InterruptedException {
        listener.getLogger().println("Checkout method Starting ..");

        final Jenkins jenkins = Jenkins.getInstance();

        if (launcher == null) {
            launcher = new LocalLauncher(listener);
        }

        FilePath repoDir;
        repoDir = workspace;

        if (!repoDir.isDirectory()) {
            repoDir.mkdirs();
        }

        final SCMAnyWhereRevisionState previousState = getLastState(build.getPreviousBuild());

        /* Get the proxy details setting from the Jenkins manage configuration */
        InetSocketAddress proxy = null;
        ProxyConfiguration proxyConfig = jenkins.proxy;
        if (proxyConfig != null) {
            Proxy myproxy = proxyConfig.createProxy(server);
            if (myproxy != null && myproxy.type() == Proxy.Type.HTTP) {
                proxy = (InetSocketAddress) myproxy.address();
            }
        }

        /* Get the latest project files to the workspace */
        ArgumentListBuilder arg = getLatestProject(workspace, proxy);
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();

        if (launcher.launch().cmds(arg).stdout(byteArray).join() != 0)
            listener.error("\nError in argument builder in Checkout\n");
        else
            listener.getLogger().print("Succesful Checkout\n");
        listener.getLogger().print("latest project files " + byteArray + "\n");

        ArgumentListBuilder changeSetArgument = getChangeSet(proxy, true);
        ByteArrayOutputStream changeSetbyteArray = new ByteArrayOutputStream();
        if (launcher.launch().cmds(changeSetArgument).stdout(changeSetbyteArray).join() != 0)
            listener.error("\nError in argument builder in getting changelog\n");
        else
            listener.getLogger().print("Succesful got the change log\n");
        // listener.getLogger().print("Get the project logging " + changeSetbyteArray + "\n");

        String loggingData = changeSetbyteArray.toString();
        String[] splitLines = loggingData.split("\n");

        int highestChangeSetID = 0;
        String dateTime = "";
        if (splitLines.length != 0) {
            highestChangeSetID = getChangeSetID(splitLines);
            dateTime = getRevisionDateTime(splitLines);
        }

        /* Get the project file detail logging from the workspace */
        ArgumentListBuilder fileDetailArgument = getChangeSet(proxy, false);
        ByteArrayOutputStream DetailbyteArray = new ByteArrayOutputStream();

        if (launcher.launch().cmds(fileDetailArgument).stdout(DetailbyteArray).pwd(workspace).join() != 0)
            listener.error("\nError in argument builder in getting changelog\n");
        else
            listener.getLogger().print("Succesful got the Detail file log\n");
        // listener.getLogger().print("project file detail " + DetailbyteArray + "\n");

        final SCMAnyWhereRevisionState currentState = new SCMAnyWhereRevisionState(String.valueOf(highestChangeSetID), dateTime);
        build.addAction(currentState);

        listener.getLogger().print("Previous State :" + previousState.getRevNo() + " " + previousState.getRevDateTime());
        listener.getLogger().print("Current State :" + highestChangeSetID + " " + dateTime);
        if (changelogFile != null) {
            SCMChangeLogParser.saveChangeLog(previousState, changelogFile, repoDir, changeSetbyteArray, DetailbyteArray, listener);
        }

    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, Map<String, String> env) {
        SCMAnyWhereRevisionState revisionState = (SCMAnyWhereRevisionState) build
                .getAction(SCMAnyWhereRevisionState.class);
        if (revisionState != null) {
            if (((SCMAnyWhereRevisionState) revisionState).getRevNo() != null) {
                env.put("SCM_REVNO_1", ((SCMAnyWhereRevisionState) revisionState).getRevNo());
                env.put("SCM_REVNO_2", ((SCMAnyWhereRevisionState) revisionState).getRevDateTime());
            }
        }
    }

    @Override
    public boolean requiresWorkspaceForPolling() {
        return false;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new SCMChangeLogParser();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends SCMDescriptor<SCMAnywhere> {

        private String scmExe;

        public DescriptorImpl() {
            super(null);
            load();
        }

        public String getDisplayName() {
            return "SCMAnywhere";
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) {
            if (req != null) req.bindJSON(this, formData);
            save();
            return true;
        }

        @Override
        public SCM newInstance(final StaplerRequest req, JSONObject formData) throws FormException {
            SCMAnywhere scm = null;
            if (req != null) scm = req.bindJSON(SCMAnywhere.class, formData);
            return scm;
        }

        /**
         * Global setting.
         *
         * @return scmExe value
         */
        public String getScmExe() {
            String scmExe = Util.fixEmptyAndTrim(this.scmExe);
            return (scmExe == null) ? "SCMSCMD" : scmExe;
        }

        /**
         * Global setting.
         *
         * @param scmExe scmExe value
         */
        public void setScmExe(String scmExe) {
            this.scmExe = scmExe;
        }

        public FormValidation doExecutableCheck(@QueryParameter final String value) {
            return FormValidation.validateExecutable(value);
        }

    }
}
