package util;

import jakarta.mail.PasswordAuthentication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class WindowsSmtpCredential {
    private WindowsSmtpCredential() {
    }

    static PasswordAuthentication get() {
        return Saved.CREDENTIAL;
    }

    private static class Saved {
        private static final PasswordAuthentication CREDENTIAL = load();
    }

    private static PasswordAuthentication load() {
        String localAppData = System.getenv("LOCALAPPDATA");
        String systemRoot = System.getenv("SystemRoot");
        if (localAppData == null || systemRoot == null
                || !Files.isRegularFile(Path.of(localAppData, "NTFashion", "smtp.credential.xml"))) {
            return null;
        }

        Path shell = Path.of(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
        String readCredential = """
                $ErrorActionPreference = 'Stop'
                [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
                $credential = Import-Clixml -LiteralPath (Join-Path $env:LOCALAPPDATA 'NTFashion/smtp.credential.xml')
                $password = $credential.GetNetworkCredential().Password -replace '\\s', ''
                if (-not $credential.UserName -or $password.Length -ne 16) { exit 1 }
                [Console]::WriteLine($credential.UserName)
                [Console]::WriteLine($password)
                """;
        try {
            Process process = new ProcessBuilder(shell.toString(), "-NoProfile", "-NonInteractive", "-Command", readCredential)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            try (var output = process.getInputStream()) {
                if (process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    List<String> lines = new String(output.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
                    if (lines.size() == 2 && !lines.get(0).isBlank() && lines.get(1).length() == 16) {
                        return new PasswordAuthentication(lines.get(0), lines.get(1));
                    }
                }
            } finally {
                if (process.isAlive()) process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
            // An unavailable local credential leaves SMTP unconfigured.
        }
        return null;
    }
}
