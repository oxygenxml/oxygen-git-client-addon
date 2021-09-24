package com.oxygenxml.git.options;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    String[] arrayFromMap = OptionsWithTags.mapToArray(map);
    
    List<String> list = Arrays.asList(arrayFromMap);
    assertEquals("[k1, v1, k2, v2, k3, v3]", list.toString());
    
    Map<String, String> mapFromArray = OptionsWithTags.arrayToMap(arrayFromMap);
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
    
    String[] tokensArray = OptionsWithTags.tokenListToArray(personalAccessTokenInfoList);
    List<String> list = Arrays.asList(tokensArray);
    assertEquals("[host1, token1, host2, token2, host3, token3]", list.toString());
    
    PersonalAccessTokenInfoList tokenInfoListFromArray = OptionsWithTags.arrayToTokenList(tokensArray);
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
    
    String[] credentialsArray = OptionsWithTags.credentialsListToArray(userAndPasswordCredentials);
    List<String> list = Arrays.asList(credentialsArray);
    assertEquals("[usrn1, pass1, host1, usrn2, pass2, host2, usrn3, pass3, host3]", list.toString());
    
    UserCredentialsList credentialsListFromArray = OptionsWithTags.arrayToCredentialsList(credentialsArray);
    assertEquals(3, credentialsListFromArray.getCredentials().size());
    assertEquals("usrn1", credentialsListFromArray.getCredentials().get(0).getUsername());
    assertEquals("pass1", credentialsListFromArray.getCredentials().get(0).getPassword());
    assertEquals("host1", credentialsListFromArray.getCredentials().get(0).getHost());
    assertEquals("usrn3", credentialsListFromArray.getCredentials().get(2).getUsername());
    assertEquals("pass3", credentialsListFromArray.getCredentials().get(2).getPassword());
    assertEquals("host3", credentialsListFromArray.getCredentials().get(2).getHost());
  }

}
