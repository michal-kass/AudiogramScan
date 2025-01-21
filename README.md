
# AudiogramScan: an Open Source mobile application for audiometry test result analysis and diagnosis support

Hearing impairments are typically assessed using pure tone audiometry, a diagnostic method that allows for the identification of the degree, type and configuration of hearing loss. The results of this assessment are generally displayed in the form of an audiogram, which graphically represents the softest sounds perceivable by an individual across a range frequencies. This paper presents a novel Open Source mobile application for the Android operating system that allows users to scan and analyse audiograms using a smartphone camera and subsequently classify the type of hearing loss. The application workflow is divided into three main stages: scanning, digitalization and classification of the audiogram. For this purpose, the application implements several artificial intelligence and image processing techniques, including YOLOv5, Optical Character Recognition (OCR) and Hough Transform. Patient diagnosis is performed by a clinically tested AI model for classification of audiometry test results. All implemented algorithms and models were optimized for functionality on mobile devices. The application was evaluated on three distinct classes of smartphones across various price points, demonstrating its efficacy and consistent performance. The presented mobile application constitutes an advanced AI-driven decision support system that is readily accessible to audiologists and otolaryngologists. Its integration in medical facilities presents a substantial opportunity to decrease clinical workload, enhance diagnostic accuracy and reduce the likelihood of human error in hearing loss evaluations, which is particularly important in developing countries.


## Authors

- Michał Kassjański
- Marcin Kulwiak
- Tomasz Przewoźny
- Dmitry Tretiakow
- Andrzej Molisz

## Tech Stack

**System:** Android

**Programming language:** Kotlin

**Min SDK / API level:** 28 (Android 9)

**Target SDK / API level:** 34 (Android 14)





## Dependencies
- [Document scanner with ML Kit on Android](https://developers.google.com/ml-kit/vision/doc-scanner/android)
- [PyTorch Mobile](https://pytorch.org/mobile/android/)
- [OpenCV on Android](https://opencv.org/android/)
- [TensorFlow Lite](https://ai.google.dev/edge/litert/libraries/task_library/overview)


## License

MIT License

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
## Acknowledgements

 - [PyTorch Android Examples](https://github.com/pytorch/android-demo-app)

