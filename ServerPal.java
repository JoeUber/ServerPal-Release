import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.concurrent.atomic.AtomicLong;

public class ServerPal extends JFrame {
    private final Preferences prefs = Preferences.userNodeForPackage(ServerPal.class);
    private JTextArea logArea;
    private Process serverProcess;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private JCheckBox autoBackupBeforeUpdateCheckBox;
    private JCheckBox checkForUpdatesOnStartCheckBox;

    public ServerPal() {
        setTitle("Palworld Server Management Tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        initComponents();
        restorePreferences();
        pack();
        if (checkForUpdatesOnStartCheckBox.isSelected()) {
            performUpdateActions();
        }
    }

    private void initComponents() {
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        JPanel settingsPanel = new JPanel();
        autoBackupBeforeUpdateCheckBox = new JCheckBox("Backup Before Update", true);
        checkForUpdatesOnStartCheckBox = new JCheckBox("Check for Updates on Start", false);
        settingsPanel.add(autoBackupBeforeUpdateCheckBox);
        settingsPanel.add(checkForUpdatesOnStartCheckBox);

        JPanel actionPanel = new JPanel();
        JButton startServerBtn = new JButton("Start Server");
        JButton checkUpdateBtn = new JButton("Check for Updates");
        JButton backupBtn = new JButton("Backup Server");
        JButton restartServerBtn = new JButton("Restart Server");
        JButton shutdownServerBtn = new JButton("Shutdown Server");

        startServerBtn.addActionListener(e -> startServer());
        checkUpdateBtn.addActionListener(e -> performUpdateActions());
        backupBtn.addActionListener(e -> performBackup());
        restartServerBtn.addActionListener(e -> restartServer());
        shutdownServerBtn.addActionListener(e -> stopServer());

        actionPanel.add(startServerBtn);
        actionPanel.add(checkUpdateBtn);
        actionPanel.add(backupBtn);
        actionPanel.add(restartServerBtn);
        actionPanel.add(shutdownServerBtn);

        setLayout(new BorderLayout());
        add(settingsPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);

        createMenuBar();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem configureSteamCMDBtn = new JMenuItem("Configure SteamCMD Path");
        JMenuItem configureServerExecutableBtn = new JMenuItem("Configure Server Executable Path");
        JMenuItem configureServerFilesBtn = new JMenuItem("Configure Server Files for Backup");
        JMenuItem configureBackupBtn = new JMenuItem("Configure Backup Path");
        JMenuItem savePreferencesItem = new JMenuItem("Save Preferences");
        JMenuItem exitItem = new JMenuItem("Exit");

        configureSteamCMDBtn.addActionListener(e -> configurePath("steamCmdPath", false));
        configureServerExecutableBtn.addActionListener(e -> configurePath("serverExecutablePath", false));
        configureServerFilesBtn.addActionListener(e -> configurePath("serverFilesPath", true));
        configureBackupBtn.addActionListener(e -> configurePath("backupPath", true));
        savePreferencesItem.addActionListener(e -> savePreferences());
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(configureSteamCMDBtn);
        fileMenu.add(configureServerExecutableBtn);
        fileMenu.add(configureServerFilesBtn);
        fileMenu.add(configureBackupBtn);
        fileMenu.add(savePreferencesItem);
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "ServerPal\nVersion 1.0\nDeveloped by UberMcKrunchy aka JoeUber\nA tool for managing your game server updates and backups.",
                "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private void configurePath(String prefKey, boolean isDirectory) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select " + prefKey);
        chooser.setFileSelectionMode(isDirectory ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String selectedPath = chooser.getSelectedFile().getAbsolutePath();
            prefs.put(prefKey, selectedPath);
            logArea.append(prefKey + " set to: " + selectedPath + "\n");
        }
    }

    private void savePreferences() {
        prefs.putBoolean("backupBeforeUpdate", autoBackupBeforeUpdateCheckBox.isSelected());
        prefs.putBoolean("checkForUpdatesOnStart", checkForUpdatesOnStartCheckBox.isSelected());
        logArea.append("Preferences saved.\n");
    }

    private void restorePreferences() {
        autoBackupBeforeUpdateCheckBox.setSelected(prefs.getBoolean("backupBeforeUpdate", false));
        checkForUpdatesOnStartCheckBox.setSelected(prefs.getBoolean("checkForUpdatesOnStart", false));
    }

    private void performUpdateActions() {
        if (autoBackupBeforeUpdateCheckBox.isSelected()) {
            performBackup();
        }
        performSteamCMDOperation(false);
    }

 synchronized private void performBackup() {
    SwingUtilities.invokeLater(() -> logArea.append("Starting backup...\n"));
    String backupPath = prefs.get("backupPath", "");
    Path sourcePath = Paths.get(prefs.get("serverFilesPath", ""));
    
    // Ensure the source directory exists and is readable
    if (!Files.exists(sourcePath) || !Files.isDirectory(sourcePath)) {
        SwingUtilities.invokeLater(() -> logArea.append("The source path does not exist or is not a directory.\n"));
        return;
    }
    
    long totalFiles = 0;
    try {
        totalFiles = countFilesInDirectory(sourcePath);
    } catch (IOException ex) {
        SwingUtilities.invokeLater(() -> logArea.append("Failed to count files in directory: " + ex.getMessage() + "\n"));
        return; // Exit the method if there's an issue accessing the directory
    }
    
    AtomicLong processedFiles = new AtomicLong(0);
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    String timeStamp = dateFormat.format(new Date());
    String zipFileName = backupPath + File.separator + "backup_" + timeStamp + ".zip";
    
    try {
        zipFolder(sourcePath, Paths.get(zipFileName), processedFiles, totalFiles);
        SwingUtilities.invokeLater(() -> logArea.append("Backup completed successfully.\n"));
    } catch (IOException ex) {
        SwingUtilities.invokeLater(() -> logArea.append("Backup failed: " + ex.getMessage() + "\n"));
    }
    }

    // Need to work on this next, I want the updates to not restart the server when the update is actually initiated, if there is no update found.. //
    private void performSteamCMDOperation(boolean forceRestart) {
    new Thread(() -> {
        try {
            String steamCmdPath = prefs.get("steamCmdPath", "");
            ProcessBuilder builder = new ProcessBuilder(steamCmdPath, "+login", "anonymous",
                    "+force_install_dir", prefs.get("serverPath", ""), "+app_update", "2394010", "validate", "+quit");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean updateStarted = false;
            boolean updateCompletedSuccessfully = false;
            while ((line = reader.readLine()) != null) {
                final String finalLine = line; // Ensure variable is effectively final for use in lambda
                SwingUtilities.invokeLater(() -> logArea.append(finalLine + "\n"));
                if (finalLine.contains("AppID 2394010 update changed : Running Update")) {
                    updateStarted = true;
                }
                if (updateStarted && finalLine.contains("AppID 2394010 scheduler finished : removed from schedule (result No Error, state 0xc)")) {
                    updateCompletedSuccessfully = true;
                }
            }
            int exitCode = process.waitFor();
            SwingUtilities.invokeLater(() -> logArea.append("SteamCMD operation completed with exit code: " + exitCode + "\n"));

            if (updateCompletedSuccessfully) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append("Update process completed. Restarting server...\n");
                    restartServer();
                });
            } else {
                SwingUtilities.invokeLater(() -> logArea.append("No update found or applied. Server continues running.\n"));
            }
        } catch (IOException | InterruptedException e) {
            SwingUtilities.invokeLater(() -> logArea.append("Failed to execute SteamCMD operation: " + e.getMessage() + "\n"));
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }
    }).start();
    }


    private void restartServer() {
        stopServer(); // Ensure server is stopped
        startServer(); // Start the server again
    }

    private void stopServer() {
    try {
        // Use the same unique window title you specified when launching the server
        String uniqueWindowTitle = "ServerPalConsole";
        // Construct a command to kill the CMD window by its title
        String killCommand = "taskkill /FI \"WINDOWTITLE eq " + uniqueWindowTitle + "\" /F /T";
        Runtime.getRuntime().exec(killCommand);
        SwingUtilities.invokeLater(() -> logArea.append("Server and its command window terminated.\n"));
        serverProcess = null; // Reset serverProcess
    } catch (IOException e) {
        SwingUtilities.invokeLater(() -> logArea.append("Failed to terminate the server and its command window: " + e.getMessage() + "\n"));
    }
    }

    private void startServer() {
    SwingUtilities.invokeLater(() -> logArea.append("Attempting to start the server...\n"));
    String serverExecutablePath = prefs.get("serverExecutablePath", "");
    if (serverExecutablePath.isEmpty()) {
        SwingUtilities.invokeLater(() -> logArea.append("Server executable path is not set.\n"));
        return;
    }
    try {
        // Check if the server process is already running
        if (serverProcess != null && serverProcess.isAlive()) {
            SwingUtilities.invokeLater(() -> logArea.append("Server is already running.\n"));
            return;
        }

        String uniqueWindowTitle = "ServerPalConsole";
        String cmdCommand = "cmd /c start \"" + uniqueWindowTitle + "\" \"" + serverExecutablePath + "\"";
        serverProcess = Runtime.getRuntime().exec(cmdCommand);

        // Log the server start without immediate check for process termination
        SwingUtilities.invokeLater(() -> logArea.append("Server started in a new command window.\n"));
        
    } catch (IOException e) {
        SwingUtilities.invokeLater(() -> logArea.append("Failed to start server: " + e.getMessage() + "\n"));
    }
    }



    private long countFilesInDirectory(Path directory) throws IOException {
        final AtomicLong count = new AtomicLong(0);
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                count.incrementAndGet();
                return FileVisitResult.CONTINUE;
            }
        });
        return count.get();
    }

    private void zipFolder(Path sourceFolderPath, Path zipPath, AtomicLong processedFiles, long totalFiles) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            zos.setLevel(Deflater.BEST_COMPRESSION);
            Files.walkFileTree(sourceFolderPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(sourceFolderPath.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    processedFiles.incrementAndGet();
                    SwingUtilities.invokeLater(() -> logArea.append("Backup progress: " + (processedFiles.get() * 100 / totalFiles) + "%\n"));
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void performBackupThenUpdate() {
        performBackup(); // Ensure backup completes before updating
        performSteamCMDOperation(true); // Force restart after backup and update check
    }

    private void scheduleAutomaticTasks() {
        // Convert 3.5 hours to milliseconds for the period
        long periodInMilliseconds = TimeUnit.MINUTES.toMillis(210); // 3.5 hours in milliseconds

        // Schedule automatic backup and update to run every 3.5 hours, starting after the first period has elapsed
        scheduler.scheduleAtFixedRate(this::performBackupThenUpdate, periodInMilliseconds, periodInMilliseconds, TimeUnit.MILLISECONDS);
    }    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerPal().setVisible(true));
    }
}