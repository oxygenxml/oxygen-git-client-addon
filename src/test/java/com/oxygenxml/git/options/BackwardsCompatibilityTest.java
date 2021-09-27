package com.oxygenxml.git.options;

import java.util.List;

import com.oxygenxml.git.OxygenGitPlugin;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

import ro.sync.exml.plugin.PluginDescriptor;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Tests how the old jaxb serialized options are migrated to the new tag based options.
 */
public class BackwardsCompatibilityTest extends GitTestBase {

  /**
   * Old format options.
   */
  private String JAXB_OPTIONS = 
      "<Options>\n"
      + "    <sshPromptAnswers>\n"
      + "        <entry>\n"
      + "            <key>Accept and store this key, and continue connecting?</key>\n"
      + "            <value>true</value>\n"
      + "        </entry>\n"
      + "        <entry>\n"
      + "            <key>Create file C:\\Users\\alex_jitianu\\.ssh\\known_hosts ?</key>\n"
      + "            <value>true</value>\n"
      + "        </entry>\n"
      + "    </sshPromptAnswers>\n"
      + "    <repositoryLocations>\n"
      + "        <location>D:\\Git-workspace\\repo1</location>\n"
      + "        <location>D:\\Git-workspace\\repo2</location>\n"
      + "    </repositoryLocations>\n"
      + "    <notifyAboutNewRemoteCommits>true</notifyAboutNewRemoteCommits>\n"
      + "    <isCheckoutNewlyCreatedLocalBranch>true</isCheckoutNewlyCreatedLocalBranch>\n"
      + "    <warnOnChangeCommitId>\n"
      + "        <entry>\n"
      + "            <key>D:\\Git-workspace\\wc1\\.git</key>\n"
      + "            <value>2232c81e2258dc8a7078bd93117c53c23a4cbce1</value>\n"
      + "        </entry>\n"
      + "        <entry>\n"
      + "            <key>D:\\Git-workspace\\wc2\\.git</key>\n"
      + "            <value>ac5ad236fd7dff448042fff98cc11e351827baf7</value>\n"
      + "        </entry>\n"
      + "    </warnOnChangeCommitId>\n"
      + "    <selectedRepository>D:\\Git-workspace\\selected-repo</selectedRepository>\n"
      + "    <userCredentials>\n"
      + "        <credential>\n"
      + "            <host>github.com</host>\n"
      + "            <username>AlexJitianu</username>\n"
      + "            <password>200;92;104;93;304;</password>\n"
      + "        </credential>\n"
      + "        <credential>\n"
      + "            <host>gitlab.sync.ro</host>\n"
      + "            <username>alex_jitianu</username>\n"
      + "            <password>ala.bala</password>\n"
      + "        </credential>\n"
      + "    </userCredentials>\n"
      + "<personalAccessTokens><personalAccessToken><host>host</host><tokenValue>token</tokenValue></personalAccessToken></personalAccessTokens>"
      + "    <commitMessages>\n"
      + "        <message>Solved conflict.</message>\n"
      + "        <message>I made a change.</message>\n"
      + "    </commitMessages>\n"
      + "    <defaultPullType>MERGE_NO_FF</defaultPullType>\n"
      + "    <projectsTested>\n"
      + "        <path>C:\\Users\\alex_jitianu\\tested-project</path>\n"
      + "    </projectsTested>\n"
      + "    <destinationPaths>\n"
      + "        <path>D:\\Git-workspace\\detination-path1</path>\n"
      + "        <path>D:\\Git-workspace\\destination-path2</path>\n"
      + "    </destinationPaths>\n"
      + "    <passphrase>pass-phrase</passphrase>\n"
      + "    <stagedResViewMode>TREE_VIEW</stagedResViewMode>\n"
      + "    <unstagedResViewMode>TREE_VIEW</unstagedResViewMode>\n"
      + "    <whenRepoDetectedInProject>AUTO_SWITCH_TO_WC</whenRepoDetectedInProject>\n"
      + "    <updateSubmodulesOnPull>false</updateSubmodulesOnPull>\n"
      + "    <isAutoPushWhenCommitting>true</isAutoPushWhenCommitting>\n"
      + "</Options>";

  @Override
  public void setUp() throws Exception {
    super.setUp();

    PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage().setOption(
        com.oxygenxml.git.options.OptionsLoader.OLD_GIT_PLUGIN_OPTIONS, 
        JAXB_OPTIONS);
    
    new OxygenGitPlugin(new PluginDescriptor());
  }

  /**
   * <p><b>Description:</b> The old jaxb-based options are loaded and copied into 
   * the new tag based options.</p>
   * <p><b>Bug ID:</b> EXM-47674</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  public void testBackwardsCompatibility() throws Exception {
    Options loadOptions = OptionsLoader.loadOptions(PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage());
    
    List<String> messages = loadOptions.getCommitMessages().getMessages();
    assertEquals(
        "Solved conflict.\n"
        + "I made a change.", messages.stream().reduce("", (a, b) -> a + "\n" + b).trim());
    
    List<String> repositoryLocations = loadOptions.getRepositoryLocations().getLocations();
    assertEquals(
        "D:\\Git-workspace\\repo1\n"
        + "D:\\Git-workspace\\repo2", repositoryLocations.stream().reduce("", (a, b) -> a + "\n" + b).trim());
    
    assertTrue(loadOptions.isAutoPushWhenCommitting());
    assertEquals(PullType.MERGE_NO_FF, loadOptions.getDefaultPullType());
    assertEquals(ResourcesViewMode.TREE_VIEW, loadOptions.getUnstagedResViewMode());
    assertEquals(ResourcesViewMode.TREE_VIEW, loadOptions.getStagedResViewMode());
    assertEquals("[D:\\Git-workspace\\detination-path1, D:\\Git-workspace\\destination-path2]", loadOptions.getDestinationPaths().getPaths().toString());
    assertEquals("[C:\\Users\\alex_jitianu\\tested-project]", loadOptions.getProjectsTestsForGit().getPaths().toString());
    assertEquals("[D:\\Git-workspace\\repo1, D:\\Git-workspace\\repo2]", loadOptions.getRepositoryLocations().getLocations().toString());
    assertTrue(loadOptions.isNotifyAboutNewRemoteCommits());
    assertTrue(loadOptions.isCheckoutNewlyCreatedLocalBranch());

    assertEquals("D:\\Git-workspace\\selected-repo", loadOptions.getSelectedRepository());
    assertEquals("[UserCredentials [host=github.com, username=AlexJitianu, password=CLASSIFIED], UserCredentials [host=gitlab.sync.ro, username=alex_jitianu, password=CLASSIFIED]]", loadOptions.getUserCredentialsList().getCredentials().toString());
    assertEquals("[Solved conflict., I made a change.]", loadOptions.getCommitMessages().getMessages().toString());
    
    assertEquals("pass-phrase", loadOptions.getPassphrase());

    assertEquals("{Accept and store this key, and continue connecting?=true, Create file C:\\Users\\alex_jitianu\\.ssh\\known_hosts ?=true}", loadOptions.getSshPromptAnswers().toString());
    
    assertEquals("AUTO_SWITCH_TO_WC", loadOptions.getWhenRepoDetectedInProject().toString());
    
    assertFalse(loadOptions.getUpdateSubmodulesOnPull());
    assertEquals("[PersonalAccessTokenInfo [host=host, personalAccessToken=token]]", loadOptions.getPersonalAccessTokensList().getPersonalAccessTokens().toString());
    assertEquals("{D:\\Git-workspace\\wc2\\.git=ac5ad236fd7dff448042fff98cc11e351827baf7, D:\\Git-workspace\\wc1\\.git=2232c81e2258dc8a7078bd93117c53c23a4cbce1}", loadOptions.getWarnOnChangeCommitId().toString());
    
  }
}
