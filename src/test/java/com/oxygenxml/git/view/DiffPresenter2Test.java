package com.oxygenxml.git.view;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;

import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;
import org.mockito.Mockito;

import com.oxygenxml.git.service.RevCommitUtilBase;
import com.oxygenxml.git.view.history.CommitCharacteristics;

public class DiffPresenter2Test {

  /**
   * <p><b>Description:</b> test the label shown for a commit in diff.</p>
   * <p><b>Bug ID:</b> EXM-54873</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testCommitInfoLabelForDiffSidePanel_1() throws Exception {
    String commitInfoLabelForDiffSidePanel = DiffPresenter.getCommitInfoLabelForDiffSidePanel(
        "DITA/topics/file.dita",
        new CommitCharacteristics(
            "my commit message",
            new Date(1728472209818l),
            "Sorinel <sorinel@sync.ro>", 
            "a1b2c3d",
            "a1b2c3dasdasdaslkdjskdsdklasjdklasjdlk",
            "Sorinel",
            Arrays.asList("z1x2y3v")));
    String expected = "<html><p>DITA/topics/file.dita – a1b2c3d</p>"
        + "<p>Sorinel &lt;sorinel@sync.ro> – my commit message – 9 Oct 2024 14:10</p></html>";
    assertEquals(expected, commitInfoLabelForDiffSidePanel);
  }
  
  /**
   * <p><b>Description:</b> test the label shown for a commit in diff.</p>
   * <p><b>Bug ID:</b> EXM-54873</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testCommitInfoLabelForDiffSidePanel_2() throws Exception {
    RevCommit commit = Mockito.mock(RevCommit.class);
    Mockito.when(commit.abbreviate(RevCommitUtilBase.ABBREVIATED_COMMIT_LENGTH))
        .thenReturn(AbbreviatedObjectId.fromString("1e90f18"));
    Mockito.when(commit.getShortMessage()).thenReturn("my commit message");
    PersonIdent personIdent = Mockito.mock(PersonIdent.class);
    Mockito.when(personIdent.getName()).thenReturn("Sorinel");
    Mockito.when(personIdent.getEmailAddress()).thenReturn("sorinel@sync.ro");
    Mockito.when(personIdent.getWhen()).thenReturn(new Date(1728472209818l));
    Mockito.when(commit.getAuthorIdent()).thenReturn(personIdent);
    String commitInfoLabelForDiffSidePanel = DiffPresenter.getCommitInfoLabelForDiffSidePanel(
        "DITA/topics/file.dita",
        commit);
    String expected = "<html><p>DITA/topics/file.dita – 1e90f18</p>"
        + "<p>Sorinel &lt;sorinel@sync.ro> – my commit message – 9 Oct 2024 14:10</p></html>";
    assertEquals(expected, commitInfoLabelForDiffSidePanel);
  }
  
}
