# Oxygen Git Client add-on

The **Git Client** add-on installs a Git client in *Oxygen XML Editor/Author/Developer*. It contributes multiple side-views, specialized for different Git operations, the main one being named **Git Staging**. The *Git Staging* side-view is available by default only in the *Editor* and *DITA* UI perspectives.

This add-on is compatible with *Oxygen XML Editor/Author/Developer* version 21.1 or higher. 

## Installation

To install the add-on, follow these instructions:

1. Go to **Help > Install new add-ons...** to open an add-on selection dialog box.
2. Enter or paste https://www.oxygenxml.com/InstData/Addons/default/updateSite.xml in the **Show add-ons from** field.
3. Select the **Git Client** add-on and click **Next**.
4. Read the end-user license agreement. Then select the **I accept all terms of the end-user license agreement** option and click **Finish**.
5. Restart the application.

**Result:** A **Git Staging** view will now be available in Oxygen. If it is not visible, go to **Window > Show View** and select **Git Staging**. This view acts as a basic Git client integrated directly in Oxygen, and it provides support for committing changes to a Git repository, comparing and merging changes, resolving conflicts, and other Git commands.

The add-on can also be installed using the following alternative procedure:
1. Go to the [Releases page](https://github.com/oxygenxml/oxygen-git-plugin/releases/latest) and download the `oxygen-git-client-{version}-plugin.jar` file.
2. Unzip it inside `{oXygenInstallDir}/plugins`. Make sure you don't create any intermediate folders. After unzipping the archive, the file system should look like this: `{oXygenInstallDir}/plugins/oxygen-git-client-{version}`, and inside this folder, there should be a `plugin.xml`file.

For more information about this add-on, read the [Git Client Add-on topic](https://www.oxygenxml.com/doc/ug-editor/topics/git-addon.html) from Oxygen's user guide.

## Copyright and License

Copyright 2021 Syncro Soft SRL.

This project is licensed under [Apache License 2.0](https://github.com/oxygenxml/oxygen-git-plugin/blob/master/LICENSE)
