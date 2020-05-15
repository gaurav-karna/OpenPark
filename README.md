# OpenPark :iphone: :no_bicycles:
### **&copy; Gaurav K. Karna 2020**

Open-sourced public parking in metropolitan areas using machine learning

![title picture](https://i.ibb.co/X3Hbcwz/Picture0.png)

## Introduction and Motivation
OpenPark is a mobile application on the Android platform that makes finding public parking more accessible and open-sourced in metropolitan and urban areas. It accomplishes this by using a proprietary object-detection algorithm developed with machine learning that is designed to extract meaningful information from pictures of parking signs. It also makes use of a proprietary string approximation algorithm to extract meaningful text from the sign. 

The information extracted consists mostly of restriction parameters, such as whether one can park or can stop between certain dates and times, permit restrictions, and the amount of time allotted to park. This information is processed and sanitized according to the aforementioned string approximation algorithm which is a specific implementation of the well-known FuzzySearch [1], and then fed into a cloud database hosted by Google Cloud Platform’s Firebase, known as Firestore. Users of the app can indirectly query this database of parking locations throughout a geographic region by opening up the map. They can also contribute to its growth using the built-in ‘Scan Sign’ function which will use the mobile camera to take a picture of a sign, compress/flatten the picture, and then extract meaning from it. The data will be mixed in with the phone’s geographic location to allow the storage of the parking sign’s location.

The motivation of the project stems from personal experiences. In the summer of 2019, I was interning at Cisco Systems Inc. in Ottawa, and would come to Montreal over the weekends to visit my friends. Every time I would come back, I would struggle to find free and accessible parking near my residence as most spots either had restrictions or were already taken. The same problem persisted back in Ottawa, a city unfamiliar to me and my fellow interns. Pursuant to this, I came up with the concept of an open-sourced map of available parking spots in the city and the idea of OpenPark was born.

OpenPark has its roots in cognitive science, with its core functionalities depending on technologies such as machine learning, object detection, and language processing. A foundational aim of machine learning is to have a general purpose computational framework to understand how humans make inferences such as recognizing meaning in text or pictures, or understand rules and conventions [2]. Similar to our brains being aware of objects and understanding language through experience, machine learning assesses data to extract patterns and make predictions.

## Tools Used and Methodology
A variety of hardware and software is being used in this project, listed below with their description and use cases:
-	[x] **Samsung Galaxy A50** – mobile smartphone being used as a development sandbox; with debug mode activated, allows live testing of camera features and application layout
-	[x] **Android Studio** – development IDE used to design the codebase and run builds and tests. Java is the programming language of choice, along with XML for layouts
-	[x] **Google Maps API** – used to provide a mapping interface to pin parking spots in different geographic regions
-	[x] **Google Firebase Firestore** – online, asynchronous, mobile-first cloud database used to store our data points about the parking signs along with their locations
-	[x] **Google Cloud Platform AutoML Vision Edge** – used to store, label, train, and evaluate the training data, as well as export the object detection model
-	[x] **Google Cloud Platform Optical Character Recognition (OCR) API** – used to extract blocks of text from a picture
-	[x] **TensorFlow Lite** – Object detection API and classifier was used to extract the labels and confidence from the output tensors after running the TensorFlow interpreter
-	[x] **GitHub** – used for source control and versioning, as well as to back up the codebase

The methodology of the project was split over three phases, that can be summarized as follows. The first phase (P1) consisted of developing the different layouts, color schemes, and base activities of the app. It also involved the app’s integration and authentication with Google Firestore, and ensuring the asynchronous querying works via acquiring API keys and enabling device permissions. Further, P1 is when the dataset was acquired. This involved me going out and taking pictures of hundreds of parking signs in the Montreal downtown area. Lastly, it included requesting the mapping interface from the Google Maps API, and ensuring the queried data is populated with pins on the resultant map.

The second phase (P2) encompassed building the camera function for the app. This function includes launching the camera intent, taking the picture, and cropping in the rectangle in which the sign is located. The resultant rectangle will be flattened into a specific Bitmap ratio, and then sent to the analysis algorithms. P2 further involved ensuring that writing to Firestore be working, as the received information from the core algorithm will be sanitized and then sent to the database to be stored.

The final phase (P3) consisted of labelling, training, and exporting the object detection model by using GCP AutoML Vision Edge (Figure 1). Training was done to prefer high accuracy results on mobile devices (Figure 2), and was done to detect 5 different labels: **(I) ‘no_stopping’**: ‘No stopping’ sign was detected, **(II) ‘no_parking’**: ‘No parking’ sign was detected, **(III) ‘parking’**: ‘Parking sign’ was detected, **(IV) ‘parking_exception’**: A parking exception was detected, and **(V) ‘sectors’**: A sector permit allowance was detected.

Further, P3 involved integrating this model into the application via using the TensorFlow Lite Object Detection API to ensure minimal latency, so that I can make sense of the output tensors (results). This integration also required the conversion of the image data into a 4-dimensional input tensor ([1, 512, 512, 3]) of unsigned 8-bit integers. This was done by converting each pixel of the cropped Bitmap (512 px by 512 px) from P2 to its native RGB values, thereby satisfying the 3-channel input tensor requirement.

![picture 1](https://i.ibb.co/fXVzdDS/Picture1.png)

**_Figure 1: Using the GCP AutoML Vision Edge platform to label our dataset_**

![picture 2](https://i.ibb.co/Yb5K7yx/Picture2.png) ![picture 3](https://i.ibb.co/CMB2KLL/Picture3.png)

**_Figure 2: Configurations for training the model on GCP AutoML Vision Edge_**

P3 also required enabling of the GCP OCR API, which returned recognized text blocks in the sign. Using the results from the object detection model, the parameter to search for in the sign became much easier. For example, an existence of sectors in our sign gives confidence in finding sector numbers in the OCR results. This search had to take into account that OCR results won’t always be perfect, and so a string approximation algorithm using the Levenshtein distance metric was used to match the results to what the sign was possibly conveying. This algorithm was used mostly in finding the time of year and days of week present in a given sign, if present. Consequently, the sanitized results are then displayed to the user, who then has the option to upload the findings to Firestore.

## Project Architecture

In this section, we’ll take a deeper look into the project’s architecture, how each of the components talk to each other, and exemplify it with how a user will interact with the app. After, we will see how the cloud database is organized using an object-oriented design. We’ll also see how this design is represented as a JSON tree with collections, documents, and their associated fields, which is the core organization principle of the Firestore (NoSQL) service.

![picture 4](https://i.ibb.co/nrGzsR5/Picture4.png)

**_Figure 3: Project Architecture Diagram_**

As can be seen in Figure 3, there are 2 main actions within OpenPark. The first of which is simply viewing the locations of the parking signs near the user’s location. Starting with arrow 1, the client sends a query request (non-blocking thread) to Firestore, which then asynchronously returns the requested data as a list of documents (all parking signs) in the collection (arrow 2), which is the current city the user is located in. Pursuant to the callback being triggered upon the data being returned from Firestore, a Google Maps Android Activity is triggered. The data is also passed along with the intent to the new Android activity. 

From here, a request is sent to the Google Maps API to return a map (arrow 3). Once returned (arrow 4), the map is populated with geotagged pins by casting the location fields from the parking spots retrieved from Firestore into java ‘LatLng’ objects. The user is then free to scroll through the map, viewing the pins near their own location. Clicking on a pin would reveal its associated restrictions within an information window.

The second main action of OpenPark is to actually read a sign and extract meaning from it. This starts by the user choosing to scan a sign, which launches a ‘Camera’ intent, followed by a ‘Crop’ intent (arrow 5). Once cropped to encompass the sign, the image is flattened to the appropriate input size (512px by 512 px) for the object detection model. At this point, the image is analyzed for the labels trained for in P3 via the object detection model and is analyzed for any text via the GCP OCR API (arrow 6). After the analysis is finished, the user can view the results on the screen (arrow 7). From here, the user has a choice of either uploading that data to the cloud (arrow 8) to make it available to all users, or simply return to the home screen (arrow 9).

Let’s now take a look at how the database is structured. See Figure 4 for reference.

![picture 5](https://i.ibb.co/mXZsxhM/Picture5.png)

**_Figure 4: Cloud database architecture for OpenPark_**

The application’s database is organized as a JSON tree, which is the standard configuration in cloud-based NoSQL services. Essentially, the database consists of several collections, each of which contain several documents. These documents can have several fields. The OpenPark database is kept extremely simple to optimize access time and ease development. We store parking spots as documents within a collection. Each parking spot is characterized with several fields, listed above in Figure 4, and all fields are stored as Strings. The values are parsed as different types according to use case within the codebase, such as converting from ‘Time’ objects for the ‘Created At’ timestamp field.

The collection in which parking spots are kept in are simply the cities in which they are located in. Inherently, this allows parking spots in different cities to have different fields. This collection is determined from the user’s city field when they take the picture that will eventually be processed into the same parking spot document. Thus, when the map is triggered, all the parking signs for a certain city are returned in a batch response from Firestore to reduce API calls.

## Results

In this section, we will delve into the results of training our object detection model over multiple trials and examine the final product by showcasing an example from within the application.

![picture 6](https://i.ibb.co/sCgVfx2/Picture6.png)

**_Figure 5: First two trials of the object detection model_**

![picture 7](https://i.ibb.co/hfwVMP8/Picture7.png)

**_Figure 6: Final trial – model used in the OpenPark application_**

In Figure 5 and Figure 6, you can observe the different results for each model configuration. Figure 6 shows model ‘**t3**’ which is the model currently used in the current release of OpenPark at **100% average precision**. Whereas in Figure 5, ‘**t1**’ is the first trial with **~82% average precision**, and ‘**t2**’ is the second trial at **~92% average precision**. Models t2 and t3 were configured for mobile use with high accuracy (highest latency), whereas model t1 was configured for mobile use with best trade-off between accuracy and latency. None of the models were deployed since OpenPark uses an offline export instead of calling the cloud API.

Prior to going through these results in detail in the ‘Discussion’ section of the report, we must understand the two metrics being displayed by the graphs – precision and recall. These metrics give us insight on how our model is manipulating data. Namely, precision tells us from the tests that were assigned a label (object was detected) – how many of them were actually supposed to be labelled [3]. Recall tells us sort of the opposite – how many tests were actually assigned the label out of the tests that should have been assigned the label [3]. Models can produce 4 outputs: (I) true positive, (II) true negative, (III) false positive, and (IV) false negative – from which precision and recall can be calculated [3].

![picture 8](https://i.ibb.co/R9WF5nn/Picture8.png)

**_Figure 7: A few examples of t3’s performance on the ‘no_parking’ label_**

We can now progress to showcasing the actual performance of model t3 in tandem with the FuzzySearch algorithm by showing the actual processing of an example sign from within the app. See Figure 8a-8b for reference.

![picture 9](https://i.ibb.co/bBF5mSM/Picture9.png)

**_Figure 8a: Taking picture of the parking sign, and cropping/flattening it to include the sign_**

![picture 10](https://i.ibb.co/dB4tqz4/Picture10.png)

**_Figure 8b: After analyzing and uploading to map; Map showing signs as blue pins with info windows upon clicking the pins_**

In Figure 8a and 8b, you can observe the aforementioned two main actions of OpenPark. The ‘no_parking’ label was detected correctly by t3, and proper string construction was done by the FuzzySearch algorithm in displaying the results from the OCR API. In the end, a coherent and more human-readable presentation of the sign information is displayed on the user interface. The information was uploaded to the map, from which any user of the application can query and view the same information displayed initially for that specific sign and any other scanned by OpenPark. 

The extra field named ‘Extra Information’ may seem a bit odd – however, it was left intentionally to include extra parameters that are not very standard across parking signs. This can include spots reserved for hotel guests, disembarking only, disabled parking, and a number of other exceptions that could not be fully classified for with the object detection model within the dataset of this project.

## Discussion

In this section, we will take a deeper look into the results, their significance, and how they were produced as a result of the core object detection algorithm and FuzzySearch string approximation algorithm. This discussion will be structured into three parts, each assessing a core tenet of the research project: (I) object detection algorithm, (II) FuzzySearch, and (III) creating the dataset.

As mentioned prior regarding the object detection algorithm, there were three different models trained. The first one was optimized for best trade-off between accuracy and latency. However, I quickly realized that the latency was not going to be a significant factor in determining the model’s configuration since the model was going to be exported to the device locally and not depend on network speed. Hence, the second model trained (t2) was optimized for high accuracy. However, there was a problem with this model as it contained two extra labels for ‘to_right’ and ‘to_left’. The initial rationale behind including these labels into the training was to account for the right or left arrows displayed on parking signs in Montreal. However, for some undetermined reason, this kept causing a drop in t2’s accuracy and led to several false positives. A logical guess to why this was happening is probably due to external factors such as the device’s rotation and the simplistic nature of the arrow. In the latter case, the model may have predicted it was simply a line of some sort (amongst several others in the rectangular cropped image), and thus could not be a possible label.

Therefore, a use-case based design decision was made here in the sense of preferring false negatives to false positives. In theory, a user would be right in front of the sign and would be able to infer what the sign is trying to convey with the help of OpenPark’s analysis. Ideally, this analysis should be perfect, but should prefer to miss conveying something (false negative) rather than conveying something that simply is not true (false positive) – where the latter is bound to confuse the user and cause distrust in the analysis. Hence, in the third training of the model, these extra labels for the right and left arrows were removed, as it was also assumed that this aspect of a parking sign is rather intuitive to a person of driving age.

Secondly, it was anticipated during the development of OpenPark that the OCR results were not going to be perfect all the time. There may be certain characters missed or misinterpreted from OCR, or that some parts of a parking sign may be occluded by foliage, construction material(s), or weathering (see Figure 9 for examples). Fortunately as humans, we are blessed with the power of context clues and inference. 

![picture 11](https://i.ibb.co/v1g0K4p/Picture11.png)

**_Figure 9: Examples of signs that can lead to trouble with OCR results due to deformities_**

However, it was clear that a string approximation algorithm was needed to ensure that even if some characters were missing – accurate predictions of what the sign was trying to convey was still possible. A critical design decision was made here to read the OCR results line by line instead of block by block, and this was done to mirror how humans read text (up to down and left to right) which is likely how the signs were designed to be read. Hence, this required using the Levenshtein distance metric encoded within the FuzzySearch algorithm to get a confidence level of how similar the OCR string is to a string that we can expect to be on a parking sign [1] [4]. A table displaying the parameters we expect to find in a sign versus the pattern used to search for similarities against the OCR string can be found in Table 1. 

Even though the FuzzySearch was not used for every parameter we store information for, a quick approach to higher accuracy was to filter against known text that did not contain information we were looking for. For example, if the String contained similarity to patterns such as “MUNIS”, “D’UN”, or “PERMIS” – the algorithm simply ignores it since it is highly unlikely that there would be critical information in that line.

![picture 12](https://i.ibb.co/PWCn8H0/Picture12.png)

**_Table 1: Parameter to validate the text for, paired with, the similarity pattern we look for_**

Finally, the dataset for OpenPark was bootstrapped and self-procured. This resulted in a set of 150 pictures of signs in and around the Montreal downtown area, close to McGill University Downtown Campus. Still, this is a relatively small dataset even though Montreal’s parking signs don’t really differ across the board in terms of their design. As it was difficult to automate this process, a best-effort remedy required manual acquisition of images using the Android A50’s camera to capture signs in different angles and under varied lighting and weather conditions. This allowed the emulation of different scenarios that users could be utilizing the application in and allowed for a high-accuracy model.

## Obstacles and Future Plans

In the development of OpenPark, there were several obstacles that had to be overcome to get to this stage. However, there are also several more such obstacles in the future for proposed features and improvements that would ultimately help the bottom line of the application. In this section, we will go over a few of these plans and discuss how they may be approached.

Firstly, OpenPark’s core machine-learning functionality, just like any other, is only as good as its dataset. Improving and expanding OpenPark’s marketable area means extending the dataset to have data on a majority portion or totality of possible signs in the metropolitan area. This will not only help users find the information they are looking for in a particular region but will also help the inner analysis engine in making better predictions. Clearly, self-procuration will only go so far in harvesting this amount of data. A possible way to tackle this is to use Google Street View, where a bot would programmatically traverse the desired market area using an API [5], taking snapshots every few feet to capture the parking signs on the sides of the street. This would not only allow mass data acquisition, but also relieve the crowdsourcing approach that OpenPark was conceived with. 

Further, at the moment, OpenPark only serves the metropolitan area of Montreal as a minimum viable product. Expansion to other cities would require a user account system with authentication and private fields such as vehicle type and current city. This would allow for the user to filter results in the OpenPark map that would only be relevant to them and would also reduce the query calls on the cloud database. Fortunately, the application currently uses Firebase as its datastore, which also comes with an authentication system [6] that can be used to register different users.

Finally, although OpenPark can now display parking spots in a certain geographical region, there should also be a way to determine if a given spot is occupied already. In theory, there is no point knowing about a possible spot if it is already taken and there is no indication of when it will be available. A possible way to address this can be in the form of a check-in system. This timer-based protocol would check in with users to ask if they are using a particular spot or not, based on the phone’s location.

## Conclusion

The idea behind OpenPark was birthed out of a need to know of accommodations in advance. Similar to how we know which hotel or Airbnb we will stay at when we travel, why not also know our car’s accommodation as well? By taking advantage of technologies such as machine learning and optical character recognition, this project aimed to address this problem. By doing so, meaningful information was able to be extracted and then uploaded automatically – making this knowledge much more accessible and open-sourced than it would have been before. In a world that is becoming increasingly urbanized [7], and is seeing an increase in car ownership [8] and utilization, such knowledge is also becoming increasingly important.

## Acknowledgements

This project would not have been possible without the supervision, guidance, and approval of:
-	**Dr. Giulia Alberini**, McGill University, Supervisor
-	**Dr. Michael Langer**, McGill University, Advisor
-	**Dr. Thomas Shultz**, McGill University, Cognitive Science Program Director
-	**Mr. Ryan Bouma**, McGill University, Cognitive Science Program Advisor

## References

[1] 	A. Cohen, "FuzzyWuzzy: Fuzzy String Matching in Python," SeatGeek, New York City, 2011.
[2] 	J. Tenenbaum, "Machine Learning and Cognitive Science," MIT Department of Brain and Cognitive Sciences, CSAIL, Cambridge, UK, 2009.
[3] 	Google Cloud Platform, "AutoML Vision's Beginner Guide," Google Inc., 10 April 2020. [Online]. Available: https://cloud.google.com/vision/automl/docs/beginners-guide?&_ga=2.49671584.-722259664.1573442407#evaluate. [Accessed 13 April 2020].
[4] 	g. (from GitHub.com), "Java fuzzy string matching implementation of the well known Python's fuzzywuzzy algorithm. Fuzzy search for Java," 6 November 2019. [Online]. Available: https://github.com/xdrop/fuzzywuzzy. [Accessed 10 April 2020].
[5] 	R. Wen, "google-streetview 1.2.9," 5 March 2019. [Online]. Available: https://pypi.org/project/google-streetview/. [Accessed 11 April 2020].
[6] 	Google Firebase, "Firebase Authentication," 28 January 2020. [Online]. Available: https://firebase.google.com/docs/auth. [Accessed 2020 11 April].
[7] 	United Nations Department of Economic and Social Affairs, "World Urbanization Prospects 2018," United Nations, New York, 2019.
[8] 	B. Schaller, "In a Reversal, ‘Car-Rich’ Households Are Growing," Citylab, New York, 2019.
[9] 	TensorFlow by Google, "TensorFlow tensors," 31 March 2020. [Online]. Available: https://www.tensorflow.org/guide/tensor. [Accessed 2020 11 April].
[10] 	TensorFlow by Google, "TensorFlow Code Examples," 31 March 2020. [Online]. Available: https://github.com/tensorflow/examples. [Accessed 9 April 2020].

**_A Gaurav Karna Production 2020_**
