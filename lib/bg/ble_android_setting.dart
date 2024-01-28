/// Android settings for BLE background service
class BLEAndroidSettings {
  const BLEAndroidSettings({
    this.notificationTitle = "Flutter BLE Background",
    this.notificationBody = "BLE Background Service",
    this.showConnections = true,
    this.scanInterval = 15,
    this.startAfterBoot = true,
  });

  /// Notification title
  final String notificationTitle;

  /// Notification body
  final String notificationBody;

  /// Show connections in notification
  final bool showConnections;

  /// Scan interval in seconds
  final int scanInterval;

  /// Start service after boot
  final bool startAfterBoot;
}
