package com.oxygenxml.git.utils;

import org.apache.commons.codec.binary.Base64;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class Cipher {

	public String encrypt(final String text) {
		return ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).getUtilAccess().encrypt(text);
	}

	public String decrypt(final String hash) {
		String decryptedPassword = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).getUtilAccess().decrypt(hash);
		if(decryptedPassword == null){
			return "";
		} else {
			return decryptedPassword;
		}
	}

}