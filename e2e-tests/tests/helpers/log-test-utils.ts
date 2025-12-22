/**
 * Log Testing Utilities
 * Provides helper functions for testing file-based logging functionality
 */

import { exec } from 'child_process';
import { promisify } from 'util';
import * as fs from 'fs/promises';
import * as path from 'path';

const execAsync = promisify(exec);

export interface LogFileInfo {
  path: string;
  size: number;
  exists: boolean;
  lastModified: Date | null;
}

export interface LogEntry {
  timestamp: string;
  level: string;
  thread: string;
  logger: string;
  message: string;
}

export interface JsonLogEntry {
  '@timestamp': string;
  level: string;
  thread_name: string;
  logger_name: string;
  message: string;
  service?: string;
}

export class LogTestUtils {
  private readonly logBasePath: string;
  private readonly services = [
    'backoffice',
    'client-portal',
    'merchant-portal',
    'organization-portal',
    'payment-service',
    'notification-service',
    'file-service',
  ];

  constructor(logBasePath: string = '/opt/kaedigital/paykesho/logs') {
    this.logBasePath = logBasePath;
  }

  /**
   * Get current date in YYYY-MM-DD format
   */
  getCurrentDateFolder(): string {
    const now = new Date();
    return now.toISOString().split('T')[0];
  }

  /**
   * Get log file path for a service
   */
  getLogFilePath(service: string, fileName: string = 'app.log', dateFolder?: string): string {
    const folder = dateFolder || 'current';
    return path.join(this.logBasePath, service, folder, fileName);
  }

  /**
   * Check if a log file exists
   */
  async logFileExists(service: string, fileName: string = 'app.log', dateFolder?: string): Promise<boolean> {
    const filePath = this.getLogFilePath(service, fileName, dateFolder);
    try {
      await fs.access(filePath);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Get log file information
   */
  async getLogFileInfo(service: string, fileName: string = 'app.log', dateFolder?: string): Promise<LogFileInfo> {
    const filePath = this.getLogFilePath(service, fileName, dateFolder);
    try {
      const stats = await fs.stat(filePath);
      return {
        path: filePath,
        size: stats.size,
        exists: true,
        lastModified: stats.mtime,
      };
    } catch {
      return {
        path: filePath,
        size: 0,
        exists: false,
        lastModified: null,
      };
    }
  }

  /**
   * Read log file contents
   */
  async readLogFile(service: string, fileName: string = 'app.log', dateFolder?: string): Promise<string> {
    const filePath = this.getLogFilePath(service, fileName, dateFolder);
    try {
      return await fs.readFile(filePath, 'utf-8');
    } catch (error) {
      throw new Error(`Failed to read log file: ${filePath}. Error: ${error}`);
    }
  }

  /**
   * Read last N lines from a log file
   */
  async readLastLines(service: string, lines: number = 100, fileName: string = 'app.log', dateFolder?: string): Promise<string> {
    const filePath = this.getLogFilePath(service, fileName, dateFolder);
    try {
      const { stdout } = await execAsync(`tail -n ${lines} ${filePath}`);
      return stdout;
    } catch (error) {
      throw new Error(`Failed to read last lines from log file: ${filePath}. Error: ${error}`);
    }
  }

  /**
   * Parse plain text log entries
   */
  parsePlainLogEntries(logContent: string): LogEntry[] {
    const entries: LogEntry[] = [];
    const lines = logContent.split('\n').filter(line => line.trim());

    const logPattern = /^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) \[([^\]]+)\] (\w+)\s+([^\s]+) - (.+)$/;

    for (const line of lines) {
      const match = line.match(logPattern);
      if (match) {
        entries.push({
          timestamp: match[1],
          thread: match[2],
          level: match[3],
          logger: match[4],
          message: match[5],
        });
      }
    }

    return entries;
  }

  /**
   * Parse JSON log entries
   */
  parseJsonLogEntries(logContent: string): JsonLogEntry[] {
    const entries: JsonLogEntry[] = [];
    const lines = logContent.split('\n').filter(line => line.trim());

    for (const line of lines) {
      try {
        const entry = JSON.parse(line);
        entries.push(entry);
      } catch {
        // Skip invalid JSON lines
      }
    }

    return entries;
  }

  /**
   * Check if log directory structure exists for a service
   */
  async checkLogDirectoryStructure(service: string): Promise<{
    baseDir: boolean;
    currentDir: boolean;
  }> {
    const baseDir = path.join(this.logBasePath, service);
    const currentDir = path.join(baseDir, 'current');

    const results = {
      baseDir: false,
      currentDir: false,
    };

    try {
      await fs.access(baseDir);
      results.baseDir = true;
    } catch {
      // Directory doesn't exist
    }

    try {
      await fs.access(currentDir);
      results.currentDir = true;
    } catch {
      // Directory doesn't exist
    }

    return results;
  }

  /**
   * Get all dated log directories for a service
   */
  async getDatedLogDirectories(service: string): Promise<string[]> {
    const serviceDir = path.join(this.logBasePath, service);
    try {
      const entries = await fs.readdir(serviceDir, { withFileTypes: true });
      return entries
        .filter(entry => entry.isDirectory() && /^\d{4}-\d{2}-\d{2}$/.test(entry.name))
        .map(entry => entry.name)
        .sort();
    } catch {
      return [];
    }
  }

  /**
   * Get archived log files (*.gz) for a service
   */
  async getArchivedLogFiles(service: string, dateFolder?: string): Promise<string[]> {
    const searchDir = dateFolder
      ? path.join(this.logBasePath, service, dateFolder)
      : path.join(this.logBasePath, service);

    try {
      const { stdout } = await execAsync(`find ${searchDir} -name "*.gz" -type f 2>/dev/null || true`);
      return stdout.split('\n').filter(line => line.trim());
    } catch {
      return [];
    }
  }

  /**
   * Count log entries with a specific log level
   */
  countLogLevel(entries: LogEntry[], level: string): number {
    return entries.filter(entry => entry.level === level).length;
  }

  /**
   * Search for log entries containing specific text
   */
  searchLogEntries(entries: LogEntry[], searchText: string): LogEntry[] {
    return entries.filter(entry =>
      entry.message.toLowerCase().includes(searchText.toLowerCase())
    );
  }

  /**
   * Trigger log generation by making requests to a service
   */
  async triggerLogGeneration(serviceUrl: string, endpoint: string = '/actuator/health', count: number = 10): Promise<void> {
    const promises = [];
    for (let i = 0; i < count; i++) {
      promises.push(
        fetch(`${serviceUrl}${endpoint}`)
          .then(() => true)
          .catch(() => false)
      );
    }
    await Promise.all(promises);
  }

  /**
   * Wait for log file to be updated
   */
  async waitForLogUpdate(
    service: string,
    fileName: string = 'app.log',
    dateFolder: string | undefined,
    timeoutMs: number = 5000
  ): Promise<boolean> {
    const startTime = Date.now();
    const filePath = this.getLogFilePath(service, fileName, dateFolder);
    let lastSize = 0;

    try {
      const stats = await fs.stat(filePath);
      lastSize = stats.size;
    } catch {
      // File doesn't exist yet
    }

    while (Date.now() - startTime < timeoutMs) {
      await new Promise(resolve => setTimeout(resolve, 500));

      try {
        const stats = await fs.stat(filePath);
        if (stats.size > lastSize) {
          return true;
        }
      } catch {
        // File doesn't exist yet
      }
    }

    return false;
  }

  /**
   * Get all services
   */
  getServices(): string[] {
    return [...this.services];
  }

  /**
   * Clean up test log files (use with caution!)
   */
  async cleanupTestLogs(olderThanDays: number = 0): Promise<void> {
    const command = `find ${this.logBasePath} -type f \\( -name "*.log" -o -name "*.gz" \\) -mtime +${olderThanDays} -delete`;
    await execAsync(command);
  }

  /**
   * Get total size of logs for a service
   */
  async getServiceLogSize(service: string): Promise<number> {
    const serviceDir = path.join(this.logBasePath, service);
    try {
      const { stdout } = await execAsync(`du -sb ${serviceDir} 2>/dev/null | cut -f1`);
      return parseInt(stdout.trim(), 10);
    } catch {
      return 0;
    }
  }
}

