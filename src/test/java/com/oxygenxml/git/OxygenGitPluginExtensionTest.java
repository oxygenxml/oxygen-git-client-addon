package com.oxygenxml.git;

import static org.junit.Assert.assertTrue;

import org.eclipse.jgit.gpg.bc.internal.BouncyCastleGpgSigner;
import org.eclipse.jgit.lib.GpgConfig.GpgFormat;
import org.eclipse.jgit.lib.Signers;
import org.junit.Test;

public class OxygenGitPluginExtensionTest {

  /**
   * <p><b>Description:</b> check the default GPG signer.</p>
   * <p><b>Bug ID:</b> EXM-52129</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testDefaultGpgSigner() throws Exception {
    assertTrue(Signers.get(GpgFormat.OPENPGP) instanceof BouncyCastleGpgSigner);
  }
  
}
