import 'package:flutter_test/flutter_test.dart';
import 'package:sms_reader/sms_reader.dart';
import 'package:sms_reader/sms_reader_platform_interface.dart';
import 'package:sms_reader/sms_reader_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockSmsReaderPlatform
    with MockPlatformInterfaceMixin
    implements SmsReaderPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final SmsReaderPlatform initialPlatform = SmsReaderPlatform.instance;

  test('$MethodChannelSmsReader is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelSmsReader>());
  });

  test('getPlatformVersion', () async {
    SmsReader smsReaderPlugin = SmsReader();
    MockSmsReaderPlatform fakePlatform = MockSmsReaderPlatform();
    SmsReaderPlatform.instance = fakePlatform;

    expect(await smsReaderPlugin.getPlatformVersion(), '42');
  });

  test('getInboxSms', () async {
    SmsReader smsReaderPlugin = SmsReader();
    MockSmsReaderPlatform fakePlatform = MockSmsReaderPlatform();
    SmsReaderPlatform.instance = fakePlatform;

    expect(await smsReaderPlugin.getInboxSms(), []);
  });
}
