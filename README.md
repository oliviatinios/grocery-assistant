# Grocery Assistant
Grocery Assistant is an Android app which helps visually impaired grocery store customers navigate around a store and find products.
This project was built for a 4th year software engineering capstone course. You can find the full presentation slides here.

## Problem & Motivation
As of now, there aren't many options for visually impaired individuals when it comes to grocery shopping. You either have to do all your shopping online or wait until an employee in store is available to help you. The latter is difficult because employees are often busy doing other tasks and having to rely on an employee reduces an individual's independence. Because of this, individuals with a visual impairement often resort to buying groceries online.

## Solution
Our solution is to build a mobile application to assist with navigation that communicates with the user entirely through voice and vibrations.

## How It Works
- Step 1: Through voice commands, the user requests directions to an item in the grocery store.
- Step 2: The backend retrieves the coordinates of the item from the database.
- Step 3: A path is calculated between the user's position and the position of the item.
- Step 4: The app guides the user to the item and alerts them when they have arrived.

## The Technology

### AWS Lex
Users communicate with the application through a voice interface. Since our application is made for users with a visual impairment, we can’t rely only on a GUI so we decided to do all our communication with the user through voice. To implement the voice interface we used [AWS Lex](https://aws.amazon.com/lex/) which is a service for building conversational interfaces. Lex has 3 main components: speech-to-text, natural language processing and text-to-speech. The speech to text component is used to transcribe an audio recording of the user’s voice into text. Lex then uses natural language processing to figure out what the user is requesting. Finally, lex takes a text response which we pre-define in our application and converts that into an audio file which is then played out loud for the user through the device’s media player.

### Bluetooth Beacons, Triangulation and Pathfinding
The navigation feature was implemented using bluetooth beacons. For our prototype of the application, 10 beacons were installed on the walls of an empty room. Each beacon emits a signal which can be picked up by the user's mobile device. In a more realistic scenario, more testing would need to be done to see how obstacles in a grocery store, such as large shelving units, would interact with the signals. Using triangulation, the position of the user's device can be located and the application will determine a path to the requested item which avoids known obstacles in the grocery store. The navigation features described above were implemented using the [Navigine SDK](https://github.com/Navigine/Android-SDK) for Android.

![Image of a bluetooth beacon](/images/beacon.jpg)

### Database
Our application’s data is stored in a NoSQL database which was created using [Amazon DynamoDB](https://aws.amazon.com/dynamodb/). We have two tables: one which stores a list of all the items in the grocery store and the coordinates of their location, and another table which stores the user’s shopping list.

### Mobile Development
The application was developed using Java and Android Studio.
