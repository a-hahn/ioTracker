#!/bin/bash
set -e  # Exit on error

# --- 1. Create User and Groups ---
echo "[1/3] Creating user and groups..."
sudo groupadd -f i2c
sudo groupadd -f leds
sudo groupadd -f adm  # For admin log access
sudo useradd -r -s /bin/false -G i2c,leds,adm iotuser 2>/dev/null || \
sudo usermod -a -G i2c,leds,adm iotuser

# --- 2. Set Up udev Rules ---
echo "[2/3] Configuring udev rules..."

# I2C access
sudo sh -c 'cat > /etc/udev/rules.d/90-i2c.rules' <<'EOL'
SUBSYSTEM=="i2c-dev", GROUP="i2c", MODE="0660"
EOL

# LED control
sudo sh -c 'cat > /etc/udev/rules.d/91-leds.rules' <<'EOL'
SUBSYSTEM=="leds", ACTION=="add", RUN+="/bin/chgrp leds /sys/class/leds/%k/brightness /sys/class/leds/%k/trigger"
SUBSYSTEM=="leds", ACTION=="add", RUN+="/bin/chmod 660 /sys/class/leds/%k/brightness /sys/class/leds/%k/trigger"
EOL

sudo udevadm control --reload-rules
sudo udevadm trigger

# --- 3. Configure Logging Directory and Service ---
echo "[3/3] Setting up service and logging..."

# Create log directory with proper permissions
sudo mkdir -p /var/log/iotracker
sudo chown iotuser:adm /var/log/iotracker
sudo chmod 775 /var/log/iotracker

# Systemd service file
sudo sh -c 'cat > /etc/systemd/system/iotracker.service' <<'EOL'
[Unit]
Description=IoTracker Service
After=network.target

[Service]
User=iotuser
Group=iotuser
WorkingDirectory=/opt/iotracker
Environment="IOTRACKER_BASE_URL=https://zmx.oops.de/VRServer/api/v1/device/din"
ExecStart=/usr/bin/java -jar ioTracker.jar

# Logging configuration
Environment="LOG_DIR=/var/log/iotracker"
SyslogIdentifier=iotracker

# Security and stability
Restart=on-failure
RestartSec=10
ReadWriteDirectories=/var/log/iotracker

[Install]
WantedBy=multi-user.target
EOL

# Application directory setup
sudo mkdir -p /opt/iotracker
sudo chown iotuser:iotuser /opt/iotracker
sudo chmod 755 /opt/iotracker

# Reload and enable
sudo systemctl daemon-reload
sudo systemctl enable iotracker.service

echo "Installation complete!"
echo "1. Place ioTracker.jar in /opt/iotracker/"
echo "2. Start service: sudo systemctl start iotracker"
echo "3. View logs:"
echo "   - sudo journalctl -u iotracker -f"
echo "   - tail -f /var/log/iotracker/iotracker.log"