package com.mytaxi.plugins;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.mytaxi.apis.phrase.tasks.PhraseAppSyncTask;
import java.io.File;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Goal which downloads all translations from phraseApp
 *
 * @author m.winkelmann
 */
@Mojo(
    name = "phrase",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresProject = true,
    requiresDependencyResolution = ResolutionScope.TEST)

@Execute(goal = "phrase")
public class PhraseAppMojo extends AbstractMojo
{

    private static final String GENERATED_RESOURCES = "/generated-resources/";

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * v2 AuthToken phrase app account. *REQUIRED
     */
    @Parameter(property = "authToken", required = true)
    private String authToken;

    /**
     * v2 ProjectId for the project you want to download the strings. *REQUIRED
     */
    @Parameter(property = "projectId", required = true)
    private String projectId;

    /**
     * Location directory of the messages folder. Default: ${project.build.directory}/generated-resources/
     */
    @Parameter(property = "generatedResourcesFolderName")
    private String generatedResourcesFolderName;

    /**
     * Location directory of the messages files. Default: ${project.build.directory + generatedResourcesFolderName}/messages
     */
    @Parameter(property = "messagesFolderName")
    private String messagesFolderName;

    /**
     * File prefix of the messages files. Default: messages_
     */
    @Parameter(property = "messageFilePrefix")
    private String messageFilePrefix;

    /**
     * File postfix of the messages files. Default: .properties
     */
    @Parameter(property = "messageFilePostfix")
    private String messageFilePostfix;

    @Component
    private BuildContext buildContext;


    @Override
    public void execute() throws MojoExecutionException
    {
        checkRequiredConfigurations();

        getLog().info("Start downlaoding message resources ...");

        PhraseAppSyncTask phraseAppSyncTask = new PhraseAppSyncTask(authToken, projectId);

        configure(phraseAppSyncTask);

        try
        {
            phraseAppSyncTask.run();
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Error in getting PhraseApp strings due build process", e);
        }

        addingCompileSource();

        getLog().info("... finished downloading message resources!");
    }


    private void addingCompileSource()
    {
        String generatedSourcePath = getGeneratedResourceFolder();
        File generatedSourcesDir = new File(generatedSourcePath);
        getLog().info("Adding " + generatedSourcePath + " to compile source.");
        project.addCompileSourceRoot(generatedSourcesDir.getAbsolutePath());
        Resource resource = new Resource();
        resource.setDirectory(generatedSourcesDir.getAbsolutePath());
        resource.setFiltering(false);
        project.addResource(resource);
        buildContext.refresh(generatedSourcesDir);
    }


    private void checkRequiredConfigurations()
    {
        getLog().info("Config: Check required configurations ...");
        Preconditions.checkNotNull("AuthToken is not configured but is REQUIRED", authToken);
        Preconditions.checkNotNull("ProjectId is not configured but is REQUIRED", projectId);
        getLog().info("Config: ... successfully checked required configurations.");
    }


    private void configure(PhraseAppSyncTask phraseAppSyncTask)
    {
        String generatedResourcesFoldername = getGeneratedResourceFolder();
        phraseAppSyncTask.setGeneratedResourcesFoldername(generatedResourcesFoldername);
        getLog().info("Config: GeneratedResourceFoldername is configured(else DEFAULT) - " + generatedResourcesFoldername);

        if (messagesFolderName != null)
        {
            getLog().info("Config: MessageFolderName is configured - " + messagesFolderName);
            phraseAppSyncTask.setMessagesFoldername(messagesFolderName);
        }
        if (messageFilePrefix != null)
        {
            getLog().info("Config: MessageFilePrefix is configured - " + messageFilePrefix);
            phraseAppSyncTask.setMessageFilePrefix(messageFilePrefix);
        }
        if (messageFilePostfix != null)
        {
            getLog().info("Config: MessageFilePostfix is configured - " + messageFilePostfix);
            phraseAppSyncTask.setMessageFilePostfix(messageFilePostfix);
        }
    }


    private String getGeneratedResourceFolder()
    {
        return project.getBuild().getDirectory() + MoreObjects.firstNonNull(generatedResourcesFolderName, GENERATED_RESOURCES);
    }

}
