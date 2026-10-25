package dev.rasm.installer;

import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import javax.swing.*;

/** Standalone installer. Never attaches to or modifies a running JVM. */
public final class RasmInstaller {
    private static final Pattern MOD_ID = Pattern.compile("\"id\"\\s*:\\s*\"rasmclient\"");
    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("--install")) {
            System.out.println(install(Path.of(args[1])));
            return;
        }
        SwingUtilities.invokeLater(RasmInstaller::showWindow);
    }

    private static void showWindow() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        JFrame frame = new JFrame("Rasm • Client setup");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBorder(BorderFactory.createEmptyBorder(24, 26, 24, 26));
        JLabel intro = new JLabel("<html><h1>RASM</h1>Install or update your client.<br><br>"
            + "Choose the Minecraft instance folder that contains <b>mods</b>.<br>"
            + "Requires Minecraft 1.21.11, Fabric, and Fabric API.<br>"
            + "Close Minecraft before installing, then restart it.<br>"
            + "This installer does not inject into an open game.</html>");
        root.add(intro, BorderLayout.NORTH);
        JTextField folder = new JTextField(defaultDirectory().toString(), 34);
        JPanel selection = new JPanel(new BorderLayout(8, 8));
        selection.add(new JLabel("Minecraft instance folder"), BorderLayout.NORTH);
        selection.add(folder, BorderLayout.CENTER);
        JButton browse = new JButton("Browse…");
        browse.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser(folder.getText());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) folder.setText(chooser.getSelectedFile().toString());
        });
        selection.add(browse, BorderLayout.EAST);
        root.add(selection, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 10));
        JLabel status = new JLabel("Existing Rasm versions are backed up; other mods stay in place.");
        footer.add(status, BorderLayout.NORTH);
        JButton install = new JButton("Install / update Rasm");
        install.addActionListener(event -> {
            install.setEnabled(false);
            String selectedFolder = folder.getText().trim();
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() throws Exception { return RasmInstaller.install(Path.of(selectedFolder)); }
                @Override protected void done() {
                    install.setEnabled(true);
                    try {
                        get();
                        status.setText("Installed. Restart Minecraft with Fabric, then press [.");
                    } catch (Exception exception) {
                        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                        JOptionPane.showMessageDialog(frame, cause.getMessage(), "Installation could not finish", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });
        footer.add(install, BorderLayout.SOUTH);
        root.add(footer, BorderLayout.SOUTH);
        frame.setContentPane(root);
        frame.pack();
        frame.setMinimumSize(frame.getSize());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static Path defaultDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) return Path.of(appData, ".minecraft");
        }
        return os.contains("mac") ? Path.of(home, "Library", "Application Support", "minecraft") : Path.of(home, ".minecraft");
    }

    static String install(Path instance) throws IOException {
        instance = instance.toAbsolutePath().normalize();
        Path mods = instance.resolve("mods");
        if (!Files.isDirectory(instance) || !Files.isDirectory(mods))
            throw new IOException("Choose an existing Minecraft instance folder containing a mods folder. Install Fabric first.");
        Path destination = mods.resolve("rasm-client-0.1.0.jar");
        List<Path> oldVersions = new ArrayList<>();
        try (var entries = Files.list(mods)) {
            for (Path file : entries.filter(p -> p.getFileName().toString().endsWith(".jar")).toList()) {
                if (isRasm(file)) oldVersions.add(file);
            }
        }
        if (Files.exists(destination) && !oldVersions.contains(destination))
            throw new IOException("The destination name is used by a different file. Nothing was replaced.");
        Path staged = Files.createTempFile(mods, ".rasm-install-", ".tmp");
        List<Path> moved = new ArrayList<>();
        Path backup = null;
        try {
            try (InputStream payload = RasmInstaller.class.getResourceAsStream("/payload/rasm-client.jar")) {
                if (payload == null) throw new IOException("Installer payload is missing. Download the installer again.");
                Files.copy(payload, staged, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!isRasm(staged)) throw new IOException("Installer payload is not a valid Rasm mod.");
            if (!oldVersions.isEmpty()) {
                Path backupRoot = instance.resolve("rasm-backups");
                Files.createDirectories(backupRoot);
                backup = Files.createTempDirectory(backupRoot, "update-");
                for (Path old : oldVersions) {
                    Files.move(old, backup.resolve(old.getFileName()));
                    moved.add(old);
                }
            }
            try { Files.move(staged, destination, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(staged, destination); }
            return "Installed " + destination + ". Restart Minecraft with Fabric and press [.";
        } catch (IOException failure) {
            for (Path old : moved) {
                try { Files.move(backup.resolve(old.getFileName()), old); }
                catch (IOException rollback) { failure.addSuppressed(rollback); }
            }
            throw failure;
        } finally { Files.deleteIfExists(staged); }
    }

    private static boolean isRasm(Path file) throws IOException {
        if (!Files.isRegularFile(file)) return false;
        try (ZipFile jar = new ZipFile(file.toFile())) {
            var metadata = jar.getEntry("fabric.mod.json");
            if (metadata == null) return false;
            try (var input = jar.getInputStream(metadata)) {
                return MOD_ID.matcher(new String(input.readNBytes(65536), java.nio.charset.StandardCharsets.UTF_8)).find();
            }
        } catch (java.util.zip.ZipException invalidJar) { return false; }
    }
}
