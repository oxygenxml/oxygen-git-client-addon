package com.oxygenxml.git.view.staging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;

import net.sf.saxon.lib.ExtensionFunctionDefinition;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * A Wizard that creates commit messages using AI Positron.
 */
public class CommitAIWizard {
  /**
   * Logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(CommitAIWizard.class);
  
  /**
   * The XSLT string used to call the AI on.
   */
  private static final String AI_XSLT = "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n"
      + "  xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
      + "  xmlns:math=\"http://www.w3.org/2005/xpath-functions/math\"\n"
      + "  exclude-result-prefixes=\"xs math\"\n"
      + "  version=\"2.0\" xmlns:ai=\"http://www.oxygenxml.com/ai/function\">\n"
      + "  <xsl:output method='text'/>\n" + "  <xsl:param name=\"system\"/>\n"
      + "  <xsl:param name=\"user\"/>\n" + "  <xsl:template match=\"/\">\n"
      + "    <xsl:value-of select=\"ai:transform-content($system, $user)\"/>\n" + "  </xsl:template>\n"
      + "</xsl:stylesheet>";
  
  /**
   * The prompt used to generate a commit message using AI.
   */
  private static final String GENERATE_COMMIT_MESSAGE = "# Context: #\n\n You are an AI specialized in software development workflows and version control systems. Your task is to analyze changes and draft precise and meaningful commit messages.\n\n # Observation: #\n\n You have access to the following information:\n- The code diff, which highlights changes between the current state and the previous commit.\n- Standard practices for creating commit messages, including format, clarity, and purpose.\n\n# Structure:#\n\n Commit messages typically include:\n- A brief summary of the changes (50 characters or less, if possible).\n- A more detailed explanation or rationale behind the changes.\n- Any relevant issue or task identifiers (e.g., \"Fixes #123\").\n- Separate title and body by a blank line.\n\n# Task: #\n\n Analyze the given code diff and generate a commit message that:\n- Clearly summarizes the changes made.\n- Follows conventional commit format.\n- Is informative yet concise.\n- Conforms to established best practices in commit messaging.\n\n#Action:#\n\nDraft the commit message suitable for use in a professional software development environment.\n\n#Result:#\nProvide the commit message in the following format:\n\n<Short Title>\n<Longer Description>\n\nDO NOT ADD markdown markers like ``` to wrap the output, only return it as is.\n\nExample Input:\n```\n--- a/sample.txt\n+++ b/sample.txt\n@@ -1,3 +1,3 @@\n-Old line of text\n+New line of modified text\n```\n\n";

  /**
   * Private constructor
   */
  private CommitAIWizard() {
    //SHOULD NOT BE USED
  }
  
  /**
   * Creates a commit message using AI.
   * 
   * @param gitAccess the git access to the repo.
   * @return The commit message.
   */
  @SuppressWarnings("deprecation")
  public static Optional<String> performAICommitCreation(GitAccess gitAccess) {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      gitAccess.getGit().diff().setCached(true).setOutputStream(outputStream).call();
      String diffs = outputStream.toString(StandardCharsets.UTF_8);

      StreamSource ss = new StreamSource(
          new StringReader(AI_XSLT));

      ExtensionFunctionDefinition def;

      def = (ExtensionFunctionDefinition) Class
          .forName("com.oxygenxml.positron.functions.AITransformContentFunction").newInstance();

      Transformer transformer = PluginWorkspaceProvider.getPluginWorkspace().getXMLUtilAccess()
          .createSaxon9HEXSLTTransformerWithExtensions(ss, new ExtensionFunctionDefinition[] { def });
      transformer.setParameter("system", GENERATE_COMMIT_MESSAGE);
      transformer.setParameter("user", diffs);
      StringWriter wr = new StringWriter();
      transformer.transform(new StreamSource(new StringReader("<root/>")), new StreamResult(wr));
      wr.close();

      return Optional.of(wr.toString());

    } catch (TransformerException | GitAPIException ex) {
      logger.error("Could not execute diff", ex);
    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
      logger.error("Could not find the AI class to generate message", ex);
    } catch (IOException ex) {
      logger.error("Could not close output stream of the AI generator", ex);
    } 
    return Optional.empty();
    
  
  }
}
