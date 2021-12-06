package com.oxygenxml.git.options;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

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
        + "CHECKOUT_COMMIT_SELECT_NEW_BRANCH", dump);
  }
}
