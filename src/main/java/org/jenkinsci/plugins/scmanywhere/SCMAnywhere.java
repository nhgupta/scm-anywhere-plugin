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
import java.util.logging.Level;
import java.util.logging.Logger;
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
	private String repositoryUrl;
	@CheckForNull
	private String port;
	@CheckForNull
	private String username;
	@CheckForNull
	private String password;
	@CheckForNull
	private String teamproject;
	private static final Logger logger = Logger.getLogger(SCMAnywhere.class.getName());
	private static final long serialVersionUID = 1L;

	@DataBoundConstructor
	public SCMAnywhere(String repositoryUrl, String port, String username, String password, String teamproject) {
		this.setPort(port);
		this.setUsername(username);
		this.setPassword(password);
		this.setTeamproject(teamproject);
		this.setRepositoryUrl(repositoryUrl);
	}

	@DataBoundSetter
	public void setTeamproject(@CheckForNull final String teamproject) {
		this.teamproject = Util.fixEmptyAndTrim(teamproject);
	}

	/**
	 * Set the IPAdress from where project need to be extracted.
	 * 
	 */
	@DataBoundSetter
	public void setRepositoryUrl(@CheckForNull final String repositoryUrl) {
		this.repositoryUrl = Util.fixEmptyAndTrim(repositoryUrl);
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
	public String getTeamProject() {
		return this.teamproject;
	}

	@Exported
	public String getRepositoryUrl() {
		return this.repositoryUrl;
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

	private SCMRevisionState getRevisionState(Launcher launcher, TaskListener listener, String root)
			throws InterruptedException {
		SCMRevisionState rev = null;
		if (rev == null) {
			logger.log(Level.WARNING, "Failed to get revision state for: {0}", root);
		}

		return rev;
	}
	
	@Override
	public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException {
		PrintStream output = listener.getLogger();
		output.println("Getting local revision...");
		SCMRevisionState local = getRevisionState(launcher, listener, build.getWorkspace().getRemote());
		output.println(local);
		return local;
	}

	private SCMAnyWhereRevisionState getLastState(final Run<?, ?> lastBuild) {
		if (lastBuild == null) {
			return null;
		}
		final SCMAnyWhereRevisionState lastState = lastBuild.getAction(SCMAnyWhereRevisionState.class);
		if (lastState != null) {
			return lastState;
		}
		return getLastState(lastBuild.getPreviousBuild());

	}

	private ArgumentListBuilder getChangeSet(String detail, String proxyhostname , int proxyport) throws IOException, InterruptedException {
		ArgumentListBuilder arg = new ArgumentListBuilder();
		arg.add(getDescriptor().getScmExe());
		arg.add("GetFolderHistory");

		if (this.repositoryUrl != "" || this.repositoryUrl != null) {
			arg.add("-server");
			arg.add(getRepositoryUrl());
		}
		if (this.port != "" || this.port != null) {
			arg.add("-port");
			arg.add(getPort());
		}

		if (this.username != "") {
			arg.add("-username");
			arg.add(getUsername());
		}

		if (this.password != "") {
			arg.add("-pwd");
			arg.add(getPassword());
		}

		if (this.teamproject != "") {
			arg.add("-teamproject");
			arg.add(getTeamProject());
		}

	if (!proxyhostname.equals("") && proxyport !=0) {
			arg.add("-ptype");
			arg.add("\"http\"");
			arg.add("-pserver");
			arg.add(proxyhostname);
			arg.add("-pport");
			arg.add(proxyport);
		}

		arg.add("-folder");
		arg.add("$/scm-anywhere");
		if (detail == "fileDetail") {
			arg.add("-v");
		}
		return arg;
	}

	@SuppressWarnings("deprecation")
	private ArgumentListBuilder getLatestProject(FilePath workspace, String proxyhostname , int proxyport) throws IOException, InterruptedException {
		
		ArgumentListBuilder arg = new ArgumentListBuilder();
		arg.add(getDescriptor().getScmExe());
		arg.add("GetLatestFolder");

		if (this.repositoryUrl != "" || this.repositoryUrl != null) {
			arg.add("-server");
			arg.add(getRepositoryUrl());
		}
		if (this.port != "" || this.port != null) {
			arg.add("-port");
			arg.add(getPort());
		}

		if (this.username != "" || this.username != null) {
			arg.add("-username");
			arg.add(getUsername());
		}

		if (this.password != "" || this.password != null) {
			arg.add("-pwd");
			arg.add(getPassword());
		}

		if (this.teamproject != "" || this.teamproject != null) {
			arg.add("-teamproject");
			arg.add(getTeamProject());
		}

		String workDir = "";
		arg.add("-workdir");
		workDir = workspace.toString().replace("\\", "/");
		arg.add(workDir);

		if (!proxyhostname.equals("") && proxyport !=0) {
			arg.add("-ptype");
			arg.add("\"http\"");
			arg.add("-pserver");
			arg.add(proxyhostname);
			arg.add("-pport");
			arg.add(proxyport);
		}
		arg.add("-folder");
		arg.add("$/scm-anywhere");
		arg.add("-r");
		return arg;
	}

	@Override
	public PollingResult compareRemoteRevisionWith(@Nonnull final Job<?, ?> job, @Nullable Launcher launcher,
			@Nullable final FilePath workspace, @Nonnull final TaskListener listener,
			@Nonnull final SCMRevisionState baseline) throws IOException, InterruptedException {
		
		PrintStream output = listener.getLogger();
		final Jenkins jenkins = Jenkins.getInstance();
		if (jenkins == null) {
			throw new IOException("Jenkins instance is not ready\n");
		}

		launcher = jenkins.createLauncher(listener);
		SCMAnyWhereRevisionState myBaseline = (SCMAnyWhereRevisionState) baseline;

		final Run<?, ?> lastRun = job.getLastBuild();
		output.printf(lastRun.getTimestampString() + lastRun.getDisplayName() + "\n");
		
		/*Get the proxy details setting from the Jenkins manage configuration */
		ProxyConfiguration proxyConfig = Jenkins.getInstance().proxy;
		String proxyhostname = "";
		int proxyport = 0;
		if (proxyConfig != null) {		
		    @SuppressWarnings("deprecation")
			Proxy proxy = proxyConfig.createProxy();	
		    if (proxy != null && proxy.type() == Proxy.Type.HTTP) {
		        InetSocketAddress address = (InetSocketAddress) proxy.address();
		        // Success, now we can get the proxy hostname and port.
		        proxyhostname = address.getHostName();
		        proxyport = address.getPort();
		    }
		}

		 if(myBaseline == null) {
			 // Probably the first build, or possibly an aborted build
		  myBaseline = getLastState(lastRun); 
		  if (myBaseline == null) 
			  output.printf("\nNo Baseline \n");
			  return PollingResult.BUILD_NOW;
		  }		

		ArgumentListBuilder arg = new ArgumentListBuilder();
		output.printf("\nGetting current remote revision...  Test with Jenkins\n");	
		arg = getChangeSet("fileDetail",proxyhostname ,proxyport);

		int highestChangeSetID = 0;
		String DateTime ="";		
		try 
		{
			ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
			if (launcher.launch().cmds(arg).stdout(byteArray).join() != 0)
				listener.error("\nError in argument builder\n");
			else
				listener.getLogger().print("Succesful pooling\n");
			listener.getLogger().print(byteArray);

			String loggingData = byteArray.toString();
			String[] splitLines = loggingData.split("\n");
			
			if(splitLines.length != 0)
			{
				highestChangeSetID = getChangeSetID(splitLines);
				DateTime = getRevisionDateTime(splitLines);
				listener.getLogger().printf("\nFirst change set : " + highestChangeSetID + " : "+  DateTime + "\n");	
			}				
		} catch (IOException e) {
			listener.error("Failed to run scm clean-tree\n");
		}
		
		
		final SCMAnyWhereRevisionState previousState = getLastState(lastRun);
		listener.getLogger().printf("Previous State :"+ previousState.getRevNo() + " "+previousState.getRevDateTime());
		listener.getLogger().printf("Current State :"+ highestChangeSetID + " "+DateTime);
		// comparing the revision state detail with the previous build
		if(!String.valueOf(highestChangeSetID).equals(previousState.getRevNo()) && !String.valueOf(DateTime).equals(previousState.getRevDateTime()))
		{
			listener.getLogger().print("build now after comparing the revisions");
			return PollingResult.BUILD_NOW;	
		}
		return PollingResult.NO_CHANGES;
	}

	/* find the latest change set ID for the project from the logging*/
	private int getChangeSetID(String[] splitLines){
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
	private String getRevisionDateTime(String[] splitLines){
		String dateTime = "";
		if (splitLines.length > 1) {
			for (String line : splitLines) {
				String[] split = line.split("\\s+");
				if (split[0].matches("-?\\d+(\\.\\d+)?")) {
					dateTime = split[2] + " "+ split[3];
					break;
				}
			}
		}
		return dateTime;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void checkout(Run<?, ?> build, Launcher launcher, FilePath workspace, TaskListener listener, 
						 File changelogFile, SCMRevisionState baseline) throws IOException, InterruptedException {
		listener.getLogger().println("Checkout method Starting ..");

		if (launcher == null) {
            launcher = new LocalLauncher(listener);
        }
		
		FilePath repoDir;
		repoDir = workspace;

		if (!repoDir.isDirectory()) {
			repoDir.mkdirs();
		}

		/*Get the proxy details setting from the Jenkins manage configuration */
		String proxyhostname = "";
		int proxyport = 0;
		ProxyConfiguration proxyConfig = Jenkins.getInstance().proxy;
		if (proxyConfig != null) {		
		    Proxy proxy = proxyConfig.createProxy();	
		    if (proxy != null && proxy.type() == Proxy.Type.HTTP) {
		        InetSocketAddress address = (InetSocketAddress) proxy.address();
		        // Success, now we can get the proxy hostname and port.
		        proxyhostname = address.getHostName();
		        proxyport = address.getPort();
		    }
		}
		
		/*Get the latest project files to the workspace*/
		ArgumentListBuilder arg = new ArgumentListBuilder();
		arg = getLatestProject(workspace, proxyhostname , proxyport);
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		 
		if (launcher.launch().cmds(arg).stdout(byteArray).join() != 0)
			listener.error("\nError in argument builder in Checkout\n");
		else
			listener.getLogger().print("Succesful Checkout\n");	
		listener.getLogger().print( "latest project files "+ byteArray + "\n");
	
		ArgumentListBuilder changeSetArgument = new ArgumentListBuilder();
		try {
			changeSetArgument = getChangeSet("fileDetail", proxyhostname , proxyport);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
		ByteArrayOutputStream changeSetbyteArray = new ByteArrayOutputStream();	
		try {
			if (launcher.launch().cmds(changeSetArgument).stdout(changeSetbyteArray).join() != 0)
				listener.error("\nError in argument builder in getting changelog\n");
			else
				listener.getLogger().print("Succesful got the change log\n");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
		//listener.getLogger().print("Get the project logging  " + changeSetbyteArray + "\n");

		String loggingData = changeSetbyteArray.toString();
		String[] splitLines = loggingData.split("\n");
			
		int highestChangeSetID = 0;
		String dateTime ="";
		if(splitLines.length !=0)
		{
			highestChangeSetID = getChangeSetID(splitLines);
			dateTime = getRevisionDateTime(splitLines);
		}
				
		/* Get the project file detail logging from the workspace */
		ArgumentListBuilder fileDetailArgument = new ArgumentListBuilder();
		fileDetailArgument = getChangeSet("NofileDetail", proxyhostname , proxyport);		
		ByteArrayOutputStream DetailbyteArray = new ByteArrayOutputStream();
		
		if (launcher.launch().cmds(fileDetailArgument).stdout(DetailbyteArray).pwd(workspace).join() != 0)
			listener.error("\nError in argument builder in getting changelog\n");
		else
			listener.getLogger().print("Succesful got the Detail file log\n");
		//listener.getLogger().print("project file detail " +DetailbyteArray + "\n");
		
		final Run<?, ?> previousBuild = build.getPreviousBuild();
		final SCMAnyWhereRevisionState previousState = getLastState(previousBuild);		
		final SCMAnyWhereRevisionState currentState =new SCMAnyWhereRevisionState(String.valueOf(highestChangeSetID) , dateTime);
		build.addAction(currentState);
		
		if (changelogFile != null) 
		{
			//listener.getLogger().printf(" ChangelogFile "+ changelogFile + "\n");
			SCMChangeLogParser.saveChangeLog(previousState, changelogFile, repoDir , changeSetbyteArray, DetailbyteArray,listener); 
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
            req.bindJSON(this, formData);
	        save();
	        return true;
	    }

		@Override
		public SCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			SCMAnywhere scm = req.bindJSON(SCMAnywhere.class, formData);
			return scm;
		}

        /**
         * Global setting.
         * @return scmExe value
         */
        public String getScmExe() {
            String scmExe = Util.fixEmptyAndTrim(this.scmExe);
            return (scmExe == null) ? "SCMSCMD" : scmExe;
        }

        /**
         * Global setting.
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