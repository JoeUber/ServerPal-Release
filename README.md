# ServerPal: Palworld Server Management Tool

## Overview

**ServerPal**, developed by **UberMcKrunchy** aka **JoeUber**, is a sophisticated graphical user interface tool designed to simplify the management of Palworld servers. It integrates **SteamCMD** for server updates, offers backup functionalities, and allows for manual server restarts and shutdowns. This tool is aimed at enhancing the server administration experience, making it more efficient and user-friendly.

## Key Features

- **Seamless SteamCMD Integration**: Automates checking and applying updates for the Palworld server.
- **Backup Management**: Facilitates easy backups of server files to safeguard data before applying updates.
- **Manual Server Control**: Includes features for restarting or shutting down the server manually for enhanced control.
- **Configurable Paths**: Allows setting paths for SteamCMD, server executable, server files for backup, and backup location within the application.
- **Preferences Saving**: Saves user settings and preferences to streamline repetitive tasks in future sessions.

## Installation and Setup

The tool comes with an easy-to-use installer to facilitate a smooth setup process. The source code is also provided for those interested in exploring or modifying the tool. Please note, the codebase is comprehensive and may present a challenge to navigate.

### Getting Started with ServerPal

1. **Run as Administrator**: It's crucial to run ServerPal with administrator rights to enable it to modify files and execute server commands without restrictions.

2. **Configuration Steps**:
   - **SteamCMD Path**: Set this by navigating to `File` > `Configure SteamCMD Path` and selecting the `steamcmd.exe` directory.
   - **Server Executable Path**: Specify the path to your server's executable.
   - **Server Files for Backup**: Choose the server files you wish to back up.
   - **Backup Path**: Determine a destination folder for the backups.

3. **Saving Preferences**: After setting up your configurations, save your preferences via `File` > `Save Preferences` to retain your settings for the next session.

4. **Server Management Features**:
   - **Check for Updates**: Initiates an update check. The server will restart only if an update is applied.
   - **Backup Server**: Manually starts a backup process for your server files.
   - **Restart Server** and **Shutdown Server**: Offers manual control over your server's operational status.

5. **Automated Functions**: ServerPal can be configured for automatic updates and backups at scheduled intervals, tailoring to your specific needs.

## Important Notes

- Make sure to select the correct `steamcmd.exe` for the SteamCMD path configuration.
- Running ServerPal in admin mode is essential for its full functionality, especially for file modifications or operations requiring elevated permissions.

## Support and Feedback

For any issues, bugs, or feedback, feel free to leave comments on the associated Reddit post for this tool. Your input is greatly appreciated and helps improve ServerPal for everyone.
