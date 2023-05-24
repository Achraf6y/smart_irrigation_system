import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_database/firebase_database.dart'
    show DatabaseReference, FirebaseDatabase;

// ignore: unused_import
import 'package:provider/provider.dart';
import 'dart:async';
import 'package:sleek_circular_slider/sleek_circular_slider.dart';

void main() async {
  Firestore firestore = firestore.initialize('irrigation-intelligente-b8c1b');
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Sensor Dashboard',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: DashboardScreen(),
    );
  }
}

class DashboardScreen extends StatefulWidget {
  @override
  _DashboardScreenState createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  late DatabaseReference _sensorRef;
  late StreamSubscription<Event> _sensorSubscription;
  late Timer _clockTimer;

  double _temperature = 0;
  double _humidity = 0;
  double _lightSensor = 0;
  double _soilMoisture = 0;
  String _currentTime = '';

  @override
  void initState() {
    super.initState();
    _sensorRef = FirebaseDatabase.instance.ref().child('data');
    _sensorSubscription = _sensorRef.onValue.listen((event) {
      var snapshot = event.snapshot;
      setState(() {
        setState(() {
          _temperature = snapshot.child('temperature').value as double;
          _humidity = snapshot.child('humidity').value as double;
          _lightSensor = snapshot.child('lightSensor').value as double;
          _soilMoisture = snapshot.child('soilMoisture').value as double;
        });
      });
    }) as StreamSubscription<Event>;
    _clockTimer = Timer.periodic(Duration(seconds: 1), (timer) {
      setState(() {
        _currentTime = DateTime.now().toString().substring(11, 19);
      });
    });
  }

  @override
  void dispose() {
    super.dispose();
    (_sensorSubscription as StreamSubscription<Event>).cancel();
    _clockTimer.cancel();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Sensor Dashboard'),
      ),
      body: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Center(
            child: Text(
              _currentTime,
              style: TextStyle(fontSize: 32.0),
            ),
          ),
          SizedBox(height: 16.0),
          SensorGauge(
            title: 'Temperature',
            value: _temperature,
            min: -20,
            max: 50,
            unit: '°C',
          ),
          SensorGauge(
            title: 'Humidity',
            value: _humidity,
            min: 0,
            max: 100,
            unit: '%',
          ),
          SensorGauge(
            title: 'Light Sensor',
            value: _lightSensor,
            min: 0,
            max: 100,
            unit: '%',
          ),
          SensorGauge(
            title: 'Soil Moisture',
            value: _soilMoisture,
            min: 0,
            max: 100,
            unit: '%',
          ),
        ],
      ),
    );
  }
}

class Event {}

class SensorGauge extends StatelessWidget {
  final String title;
  final double value;
  final double min;
  final double max;
  final String unit;

  SensorGauge({
    required this.title,
    required this.value,
    required this.min,
    required this.max,
    required this.unit,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.all(16.0),
      child: Padding(
        padding: EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: TextStyle(fontSize: 24.0, fontWeight: FontWeight.bold),
            ),
            SleekCircularSlider(
              appearance: CircularSliderAppearance(),
              min: min,
              max: max,
              initialValue: value,
              innerWidget: (value) {
                return Center(
                  child: Text(
                    '${value.round()}$unit',
                    style: TextStyle(fontSize: 24.0),
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}
