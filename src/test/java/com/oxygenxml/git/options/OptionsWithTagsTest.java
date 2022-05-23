package com.oxygenxml.git.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * Used for testing the methods in OptionsWithTags
 * 
 * @author gabriel_nedianu
 *
 */
public class OptionsWithTagsTest {

  @Test
  public void testArrayToMapAndViceVersa() {
    
    Map<String, String> map = new HashMap<>();
    map.put("k1", "v1");
    map.put("k2", "v2");
    map.put("k3", "v3");
    String[] arrayFromMap = TagBasedOptions.mapToArray(map);
    
    List<String> list = Arrays.asList(arrayFromMap);
    assertEquals("[k1, v1, k2, v2, k3, v3]", list.toString());
    
    Map<String, String> mapFromArray = TagBasedOptions.arrayToMap(arrayFromMap);
    assertEquals("{k1=v1, k2=v2, k3=v3}", mapFromArray.toString());
  }
  
  @Test
  public void testArrayToTokenListAndViceVersa() {
    PersonalAccessTokenInfoList personalAccessTokenInfoList = new PersonalAccessTokenInfoList();
    List<PersonalAccessTokenInfo> tokensList = new ArrayList<>();
    for(int i = 1; i < 4; i++) {
      PersonalAccessTokenInfo tokenInfo = new PersonalAccessTokenInfo("host" + i, "token" + i);
      tokensList.add(tokenInfo);
    }
    personalAccessTokenInfoList.setPersonalAccessTokens(tokensList);
    
    String[] tokensArray = TagBasedOptions.tokenListToArray(personalAccessTokenInfoList);
    List<String> list = Arrays.asList(tokensArray);
    assertEquals("[host1, token1, host2, token2, host3, token3]", list.toString());
    
    PersonalAccessTokenInfoList tokenInfoListFromArray = TagBasedOptions.arrayToTokenList(tokensArray);
    assertEquals(3, tokenInfoListFromArray.getPersonalAccessTokens().size());
    assertEquals("host1", tokenInfoListFromArray.getPersonalAccessTokens().get(0).getHost());
    assertEquals("token1", tokenInfoListFromArray.getPersonalAccessTokens().get(0).getTokenValue());
    assertEquals("host2", tokenInfoListFromArray.getPersonalAccessTokens().get(1).getHost());
    assertEquals("token2", tokenInfoListFromArray.getPersonalAccessTokens().get(1).getTokenValue());
    assertEquals("host3", tokenInfoListFromArray.getPersonalAccessTokens().get(2).getHost());
    assertEquals("token3", tokenInfoListFromArray.getPersonalAccessTokens().get(2).getTokenValue());
  }
  
  @Test
  public void testArrayToCredentialsListAndViceVersa() {
    UserCredentialsList userAndPasswordCredentials = new UserCredentialsList();

    List<UserAndPasswordCredentials> credentialsList = new ArrayList<>();
    
    for(int i = 1; i < 4; i++) {
      UserAndPasswordCredentials uapc = new UserAndPasswordCredentials("usrn" + i, "pass" + i, "host" + i);
      credentialsList.add(uapc);
    }
    userAndPasswordCredentials.setCredentials(credentialsList);
    
    String[] credentialsArray = TagBasedOptions.credentialsListToArray(userAndPasswordCredentials);
    List<String> list = Arrays.asList(credentialsArray);
    assertEquals("[usrn1, pass1, host1, usrn2, pass2, host2, usrn3, pass3, host3]", list.toString());
    
    UserCredentialsList credentialsListFromArray = TagBasedOptions.arrayToCredentialsList(credentialsArray);
    assertEquals(3, credentialsListFromArray.getCredentials().size());
    assertEquals("usrn1", credentialsListFromArray.getCredentials().get(0).getUsername());
    assertEquals("pass1", credentialsListFromArray.getCredentials().get(0).getPassword());
    assertEquals("host1", credentialsListFromArray.getCredentials().get(0).getHost());
    assertEquals("usrn3", credentialsListFromArray.getCredentials().get(2).getUsername());
    assertEquals("pass3", credentialsListFromArray.getCredentials().get(2).getPassword());
    assertEquals("host3", credentialsListFromArray.getCredentials().get(2).getHost());
  }
  
  /**
   * <p><b>Description:</b> Used to test if the credentials 
   * for an already existing host are saved correctly.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50371</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  @Test
  public void testUpdateHostCredentials() throws Exception {
    UtilAccess utilAccess = Mockito.mock(UtilAccess.class);
    Mockito.when(utilAccess.encrypt(Mockito.any(String.class))).then((Answer<String>) 
        invocation -> {
      return invocation.getArgument(0, String.class);
    });
    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    Mockito.when(pluginWSMock.getUtilAccess()).thenReturn(utilAccess);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
   
    for(int i = 1; i < 6; i++) {
      PersonalAccessTokenInfo tokenInfo = new PersonalAccessTokenInfo("host" + i, "token" + i);
      OptionsManager.getInstance().saveGitCredentials(tokenInfo);
    }
    UserAndPasswordCredentials uapc1 = new UserAndPasswordCredentials(
        "usrn" + 2, "pass" + 2, "host" + 2);
    UserAndPasswordCredentials uapc2 = new UserAndPasswordCredentials(
        "usrn" + 4, "pass" + 4, "host" + 4);
    OptionsManager.getInstance().saveGitCredentials(uapc1);
    OptionsManager.getInstance().saveGitCredentials(uapc2);
    
    List<UserAndPasswordCredentials> credentials = new ArrayList<>(
        OptionsManager.getInstance().getOptions().getUserCredentialsList()
        .getCredentials());
    List<PersonalAccessTokenInfo> personalAccessTokens = 
        new ArrayList<>(OptionsManager.getInstance().getOptions()
            .getPersonalAccessTokensList().getPersonalAccessTokens() );
  
    assertEquals(2, credentials.size());
    assertEquals(3, personalAccessTokens.size());   
    for(int i = 1; i < 6; i += 2) {
      assertEquals("host" + i, personalAccessTokens.get(i / 2).getHost());
      assertEquals("token" + i, personalAccessTokens.get(i / 2).getTokenValue());
    }
    for(int i = 4; i > 1; i -= 2) {
      assertEquals("host" + i, credentials.get(i / 2 - 1).getHost());
      assertEquals("pass" + i, credentials.get(i / 2 - 1).getPassword());
      assertEquals("usrn" + i, credentials.get(i / 2 - 1).getUsername());
    }
    PersonalAccessTokenInfo tokenInfo = new PersonalAccessTokenInfo("host" + 3, "token_of_life");
    OptionsManager.getInstance().saveGitCredentials(tokenInfo);
    credentials = new ArrayList<>(
        OptionsManager.getInstance().getOptions().getUserCredentialsList()
        .getCredentials());
    personalAccessTokens =  new ArrayList<>(OptionsManager.getInstance().getOptions()
            .getPersonalAccessTokensList().getPersonalAccessTokens() );
    assertFalse(credentials.stream().anyMatch(c -> "host3".equals(c.getHost())));
    assertTrue(personalAccessTokens.stream().anyMatch(
        t -> "host3".equals(t.getHost()) && "token_of_life".equals(t.getTokenValue())));
  }
  
  /**
   * <p><b>Description:</b> Used to test if the credentials are correctly reseted.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50371</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  @Test
  public void testResetCretentials() {
    UtilAccess utilAccess = Mockito.mock(UtilAccess.class);
    Mockito.when(utilAccess.encrypt(Mockito.any(String.class))).then((Answer<String>) 
        invocation -> {
      return invocation.getArgument(0, String.class);
    });
    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    Mockito.when(pluginWSMock.getUtilAccess()).thenReturn(utilAccess);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
   
    for(int i = 1; i < 4; i++) {
      PersonalAccessTokenInfo tokenInfo = new PersonalAccessTokenInfo("host" + i, "token" + i);
      OptionsManager.getInstance().saveGitCredentials(tokenInfo);
    }
    
    for(int i = 4; i < 8; i++) {
      UserAndPasswordCredentials uapc = new UserAndPasswordCredentials(
          "usrn" + i, "pass" + i, "host" + i);
      OptionsManager.getInstance().saveGitCredentials(uapc);
    }
    
    List<UserAndPasswordCredentials> credentials = new ArrayList<>(
        OptionsManager.getInstance().getOptions().getUserCredentialsList()
        .getCredentials());
    List<PersonalAccessTokenInfo> personalAccessTokens = 
        new ArrayList<>(OptionsManager.getInstance().getOptions()
            .getPersonalAccessTokensList().getPersonalAccessTokens() );
  
    assertEquals(4, credentials.size());
    assertEquals(3, personalAccessTokens.size());
    
    OptionsManager.getInstance().saveGitCredentials(null);
    
    credentials = new ArrayList<>(
        OptionsManager.getInstance().getOptions().getUserCredentialsList()
        .getCredentials());
    personalAccessTokens = 
        new ArrayList<>(OptionsManager.getInstance().getOptions()
            .getPersonalAccessTokensList().getPersonalAccessTokens() );
  
    assertEquals(0, credentials.size());
    assertEquals(0, personalAccessTokens.size());
    
  }

  /**
   * <p><b>Description:</b> If a new option is added, verify if it should be saved 
   * at project level.</p>
   * <p><b>Bug ID:</b> EXM-47674</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception if it fails.
   */
  @Test
  public void testProjectLevelOptions() throws Exception {
    Field[] fields = OptionTags.class.getFields();
    
    String dump = Arrays.stream(fields).map((Field f) -> f.getName()).collect(Collectors.joining("\n"));
    assertEquals(
        "A new option was added. If the option is displayed in the preferences page OxygenGitOptionPagePluginExtension then "
        + " it must be placed in OxygenGitOptionPagePluginExtension.getProjectLevelOptionKeys() to save it at project level.",
        "AUTO_PUSH_WHEN_COMMITTING\n"
        + "NOTIFY_ABOUT_NEW_REMOTE_COMMITS\n"
        + "CHECKOUT_NEWLY_CREATED_LOCAL_BRANCH\n"
        + "SELECTED_REPOSITORY\n"
        + "REPOSITORY_LOCATIONS\n"
        + "DESTINATION_PATHS\n"
        + "DEFAULT_PULL_TYPE\n"
        + "UNSTAGED_RES_VIEW_MODE\n"
        + "STAGED_RES_VIEW_MODE\n"
        + "PROJECTS_TESTED_FOR_GIT\n"
        + "USER_CREDENTIALS_LIST\n"
        + "COMMIT_MESSAGES\n"
        + "PASSPHRASE\n"
        + "WHEN_REPO_DETECTED_IN_PROJECT\n"
        + "UPDATE_SUBMODULES_ON_PULL\n"
        + "WARN_ON_CHANGE_COMMIT_ID\n"
        + "SSH_PROMPT_ANSWERS\n"
        + "PERSONAL_ACCES_TOKENS_LIST\n"
        + "STASH_INCLUDE_UNTRACKED\n"
        + "CHECKOUT_COMMIT_SELECT_NEW_BRANCH\n"
        + "HISTORY_STRATEGY\n"
        + "VALIDATE_FILES_BEFORE_COMMIT\n"
        + "REJECT_COMMIT_ON_VALIDATION_PROBLEMS\n"
        + "VALIDATE_MAIN_FILES_BEFORE_PUSH\n" + 
        "REJECT_PUSH_ON_VALIDATION_PROBLEMS", dump);
  }
}
