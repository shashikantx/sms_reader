import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'sms_reader_method_channel.dart';

abstract class SmsReaderPlatform extends PlatformInterface {
  /// Constructs a SmsReaderPlatform.
  SmsReaderPlatform() : super(token: _token);

  static final Object _token = Object();

  static SmsReaderPlatform _instance = MethodChannelSmsReader();

  /// The default instance of [SmsReaderPlatform] to use.
  ///
  /// Defaults to [MethodChannelSmsReader].
  static SmsReaderPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [SmsReaderPlatform] when
  /// they register themselves.
  static set instance(SmsReaderPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<List<Map<Object?, Object?>>> readInbox(
      {int? page, int? pageSize, String? searchQuery, String? sortOrder}) {
    throw UnimplementedError('readInbox() has not been implemented.');
  }

  Future<bool> canReadSms() {
    throw UnimplementedError('canReadSms() has not been implemented.');
  }
}
