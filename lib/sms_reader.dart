import 'sms_reader_platform_interface.dart';

class SmsReader {
  Future<String?> getPlatformVersion() {
    return SmsReaderPlatform.instance.getPlatformVersion();
  }

  Future<List<Map<Object?, Object?>>> getInboxSms(
      {int? page, int? pageSize, String? searchQuery}) {
    return SmsReaderPlatform.instance
        .getInboxSms(page: page, pageSize: pageSize, searchQuery: searchQuery);
  }
}
