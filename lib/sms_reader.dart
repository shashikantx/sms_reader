import 'sms_reader_platform_interface.dart';

class SmsReader {
  Future<List<Map<Object?, Object?>>> readInbox(
      {int? page, int? pageSize, String? searchQuery, String? sortOrder}) {
    return SmsReaderPlatform.instance.readInbox(
      page: page,
      pageSize: pageSize,
      searchQuery: searchQuery,
      sortOrder: sortOrder,
    );
  }

  Future<bool> canReadSms() {
    return SmsReaderPlatform.instance.canReadSms();
  }
}
