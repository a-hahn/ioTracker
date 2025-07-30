#!/bin/bash
set -e  # Exit on error

# --- 1. Create User and Groups ---
echo "[1/3] Creating user and groups..."
sudo groupadd -f i2c
sudo groupadd -f leds
sudo useradd -r -s /bin/false -G i2c,leds iotuser

# --- 2. Set Up udev Rules ---
echo "[2/3] Configuring udev rules..."

# I2C access
sudo sh -c 'cat > /etc/udev/rules.d/90-i2c.rules' <<EOL
SUBSYSTEM=="i2c-dev", GROUP="i2c", MODE="0660"
EOL

# LED control
sudo sh -c 'cat > /etc/udev/rules.d/91-leds.rules' <<EOL
SUBSYSTEM=="leds", ACTION=="add", RUN+="/bin/chgrp leds /sys/class/leds/%k/brightness /sys/class/leds/%k/trigger"
SUBSYSTEM=="leds", ACTION=="add", RUN+="/bin/chmod 660 /sys/class/leds/%k/brightness /sys/class/leds/%k/trigger"
EOL

sudo udevadm control --reload-rules
sudo udevadm trigger

# --- 3. Configure Enhanced Logging ---
echo "[3/3] Setting up logging with timestamps..."

# Single log file with rotation
sudo sh -c 'cat > /etc/logrotate.d/iotracker' <<EOL
/var/log/iotracker.log {
    weekly
    missingok
    rotate 4
    compress
    delaycompress
    notifempty
    create 644 iotuser iotuser
}
EOL

sudo touch /var/log/iotracker.log
sudo chown iotuser:iotuser /var/log/iotracker.log
sudo chmod 644 /var/log/iotracker.log

# Systemd service with JAVA-level timestamps
sudo sh -c 'cat > /etc/systemd/system/iotracker.service' <<EOL
[Unit]
Description=IoTracker Java Application
After=network.target

[Service]
User=iotuser
Group=iotuser
WorkingDirectory=/opt/iotracker

# Combined output with systemd timestamps
SyslogIdentifier=iotracker

# Java command with application-level timestamps
ExecStart=/usr/bin/java -jar ioTracker.jar

Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOL

# Application directory
sudo mkdir -p /opt/iotracker
sudo chown iotuser:iotuser /opt/iotracker
sudo chmod 755 /opt/iotracker

sudo systemctl daemon-reload
sudo systemctl enable iotracker.service

echo "Installation complete!"
echo "1. Place 'ioTracker.jar' in /opt/iotracker/"
echo "2. Start service: sudo systemctl start iotracker"
echo "3. View logs: sudo tail -f /var/log/iotracker.log"
