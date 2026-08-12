package com.abnormalities.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class WindErrorClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|TheWind|Error");
    private static volatile boolean dialogOpen = false;

    public static void triggerError(String title, String message, float eventMultiplier) {
        if (dialogOpen) return;
        dialogOpen = true;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            dialogOpen = false;
            return;
        }

        LOGGER.info("[THE_WIND|Error] Triggering real OS dialog");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            JDialog dialog = new JDialog((Frame) null, title, true);
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    doForceClose(dialog);
                }
                @Override
                public void windowActivated(WindowEvent e) {
                    dialog.toFront();
                }
            });

            int width = 440;
            int height = 200;
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            dialog.setLocation((screen.width - width) / 2, (screen.height - height) / 2);
            dialog.setSize(width, height);
            dialog.setResizable(false);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(new EmptyBorder(15, 15, 15, 15));
            panel.setBackground(UIManager.getColor("Panel.background"));

            JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            topRow.setOpaque(false);
            topRow.setMaximumSize(new Dimension(440, 50));

            JLabel iconLabel = new JLabel("\u26A0");
            iconLabel.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 32));
            topRow.add(iconLabel);

            JLabel msgLabel = new JLabel("<html><div style='width:340px'><b>" + escapeHtml(title) + "</b><br><br>" + escapeHtml(message) + "</div></html>");
            msgLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
            topRow.add(msgLabel);

            panel.add(topRow);
            panel.add(Box.createVerticalGlue());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttonPanel.setOpaque(false);
            buttonPanel.setMaximumSize(new Dimension(440, 40));

            JButton dismissBtn = new JButton("Dismiss");
            dismissBtn.setPreferredSize(new Dimension(85, 26));
            dismissBtn.addActionListener(e -> doDismiss(dialog));

            JButton closeBtn = new JButton("Close the program");
            closeBtn.setPreferredSize(new Dimension(140, 26));
            closeBtn.addActionListener(e -> doForceClose(dialog));

            buttonPanel.add(dismissBtn);
            buttonPanel.add(closeBtn);
            panel.add(buttonPanel);

            dialog.setContentPane(panel);
            dialog.setVisible(true);
            dialog.toFront();
            dialog.requestFocus();
        });
    }

    private static void doDismiss(JDialog dialog) {
        dialog.dispose();
        dialogOpen = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            com.abnormalities.AbnormalitiesMod.CHANNEL.sendToServer(
                new com.abnormalities.network.WindErrorDismissPacket());
        }

        LOGGER.info("[THE_WIND|Error] Dialog dismissed");
    }

    private static void doForceClose(JDialog dialog) {
        dialog.dispose();
        dialogOpen = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.stop();
        }

        LOGGER.info("[THE_WIND|Error] Dialog force-closed");
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
