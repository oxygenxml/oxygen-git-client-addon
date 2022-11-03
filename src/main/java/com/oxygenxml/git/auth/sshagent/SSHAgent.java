package com.oxygenxml.git.auth.sshagent;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;

/**
 * Contains all supported SSH agents.
 * 
 * @author alex_smarandache
 *
 */
public enum SSHAgent {

  /**
   * Windows Pageagent SSH.
   */
  WIN_PAGEAGENT("Pageagent", "\\\\.\\pageant"),

  /**
   * Windows Win32 OpenSSh.
   */
  WIN_WIN32_OPENSSH("Win32 Open SSH", "\\\\.\\pipe\\openssh-ssh-agent"),

  /**
   * Unix Default SSH agent.
   */
  UNIX_DEFAULT_SSH_AGENT("OpenSSH", "?");

  /**
   * The SSH agent name.
   */
  private String name;

  /**
   * The identity of the agent.
   */
  private String identityAgent;

  /**
   * Constructor.
   * 
   * @param name             The SSH agent name.
   * @param identityAgent    The identity of the agent.
   */
  private SSHAgent(@NonNull final String name, @NonNull final String identityAgent) {
    this.name = name;
    this.identityAgent = identityAgent;
  }

  /**
   * @return The identity of the agent.
   */
  public String getIdentityAgent() {
    return identityAgent;
  }

  /**
   * @return The name of the agent.
   */
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return getName();
  }

  /**
   * Check if the given agent is for Windows or not.
   * 
   * @param sshAgent The given SSH agent.
   * 
   * @return <code>true</code> if the agent is for Windows.
   */
  public static boolean isForWin(final SSHAgent sshAgent) {
    return WIN_PAGEAGENT == sshAgent || WIN_WIN32_OPENSSH == sshAgent;
  }

  /**
   * @return All Windows SSH agents supported.
   */
  public static SSHAgent[] getWindowsSSHAgents() {
    final SSHAgent[] agents= {WIN_PAGEAGENT, WIN_WIN32_OPENSSH};
    return agents;
  }

  /**
   * @return All Unix agents supported.
   */
  public static SSHAgent[] getUnixSSHAgents() {
    final SSHAgent[] agents= {UNIX_DEFAULT_SSH_AGENT};
    return agents;
  }

  /**
   * Search and return the SSH agent with the given name.
   * 
   * @param name The name of the SSH agent.
   * 
   * @return Found SSH Agent.
   */
  @Nullable
  public static SSHAgent getByName(final String name) {
    SSHAgent toReturn;
    switch(name) {
      case "Pageagent": {
        toReturn = SSHAgent.WIN_PAGEAGENT;
        break;
      }
      case "Win32 Open SSH": {
        toReturn = SSHAgent.WIN_WIN32_OPENSSH;
        break;
      }
      case "OpenSSH": {
        toReturn =  SSHAgent.UNIX_DEFAULT_SSH_AGENT;
        break;
      } 
      default : {
        toReturn = null;
      }
    }
    return toReturn;
  }

}
