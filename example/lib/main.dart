import 'dart:ffi';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:sms_reader/sms_reader.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final scrollController = ScrollController();
  final _smsReaderPlugin = SmsReader();
  final _searchController = TextEditingController();

  List<Map<Object?, Object?>> _inboxSms = [];
  int page = 0;
  bool isLoading = false;
  bool hasMore = true;
  String? searchQuery;

  @override
  void initState() {
    scrollController.addListener(() {
      if (scrollController.position.pixels >=
          scrollController.position.maxScrollExtent) {
        loadMore();
      }
    });
    super.initState();
    initialLoad();
  }

  @override
  void dispose() {
    scrollController.dispose();
    super.dispose();
  }

  Future<void> initialLoad() async {
    //try reading sms
    try {
      List<Map<Object?, Object?>> inboxSms =
          await _smsReaderPlugin.readInbox(page: page);
      setState(() {
        _inboxSms = inboxSms;
      });
    } on PlatformException {
      print('Failed to get inbox sms.');

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Failed to get inbox sms.'),
          ),
        );
      }
    }

    if (!mounted) return;
  }

  void reset() {
    setState(() {
      _inboxSms = [];
      page = 0;
      isLoading = false;
      hasMore = true;
    });
  }

  //load more sms
  void loadMore() async {
    setState(() {
      isLoading = true;
    });
    if (!hasMore) {
      setState(() {
        isLoading = false;
      });
      return;
    }

    page++;
    List<Map<Object?, Object?>> inboxSms =
        await _smsReaderPlugin.readInbox(page: page, searchQuery: searchQuery);
    if (inboxSms.isEmpty) {
      hasMore = false;
    }

    if (inboxSms.isNotEmpty) {
      setState(() {
        _inboxSms = [..._inboxSms, ...inboxSms];
        isLoading = false;
      });
    } else {
      setState(() {
        isLoading = false;
      });
    }
  }

  void search() async {
    // Platform messages may fail, so we use a try/catch PlatformException.
    // reset page
    reset();
    try {
      page = 0;
      searchQuery = _searchController.text;
      List<Map<Object?, Object?>> inboxSms = await _smsReaderPlugin.readInbox(
          page: page, searchQuery: searchQuery);
      setState(() {
        _inboxSms = inboxSms;
      });
    } on PlatformException {
      print('Failed to get inbox sms.');
    }
  }

  void clearSearch() async {
    // Platform messages may fail, so we use a try/catch PlatformException.
    // reset page
    reset();
    try {
      searchQuery = _searchController.text;
      List<Map<Object?, Object?>> inboxSms = await _smsReaderPlugin.readInbox(
          page: page, searchQuery: searchQuery);
      setState(() {
        _inboxSms = inboxSms;
      });

      scrollController.animateTo(scrollController.position.minScrollExtent,
          duration: const Duration(seconds: 1), curve: Curves.bounceIn);
      _searchController.clear();
    } on PlatformException {
      print('Failed to get inbox sms.');
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('SMS Reader Plugin example app'),
        ),
        body: Column(
          children: [
            //search bar
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: TextField(
                controller: _searchController,
                onChanged: (value) => {
                  searchQuery = value,
                },
                decoration: InputDecoration(
                  hintText: 'Search',
                  prefixIcon: IconButton(
                    onPressed: clearSearch,
                    icon: const Icon(Icons.clear),
                  ),
                  suffixIcon: IconButton(
                    onPressed: search,
                    icon: const Icon(Icons.search),
                  ),
                ),
              ),
            ),
            // list
            Expanded(
              flex: 1,
              child: ListView.builder(
                controller: scrollController,
                itemCount: isLoading ? _inboxSms.length + 1 : _inboxSms.length,
                itemBuilder: (context, index) {
                  if (index == _inboxSms.length) {
                    if (isLoading) {
                      return const Center(
                        child: CircularProgressIndicator(),
                      );
                    }
                  }
                  return ListTile(
                    title: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text((_inboxSms[index]['address'] as String?) ?? ''),
                        Text(DateTime.fromMillisecondsSinceEpoch(int.parse(
                                (_inboxSms[index]['date'] as String?) ?? '0'))
                            .toString()),
                      ],
                    ),
                    subtitle: Text((_inboxSms[index]['body'] as String?) ?? ''),
                  );
                },
              ),
            )
          ],
        ),
      ),
    );
  }
}
