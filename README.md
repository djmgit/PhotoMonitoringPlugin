PhotoMonitoringPlugin
=====================

Description:
The photo monitoring plugins are written to work with Fiji image processing software (http://fiji.sc/wiki/index.php/Fiji) and they will also work with ImageJ the software on which Fiji is based. These plugins are designed to improve the efficiency and effectiveness of using photo monitoring methods. The current plugins are focused on support dual-camera setups with one camera acquiring a "normal" visible color digital photo and the other acquiring a near-infrared digitial photo. 

There are currently two plugins bundled in PhotoMonitoringPlugin:
1 - Create a file with file names for image pairs
The Create list plugin is designed to facilitate the process of matching digital photographs that were acquired at roughly the same time. The plugin outputs a text file with the path and file names for image pairs (e.g., images acquired from two cameras) that can be input into the Reg images plugin. The image matching is done by synchronizing the times stored in the image EXIF DateTimeOriginal tag from each of two cameras. If for some reason the EXIF DateTimeOriginal tag is not set then the files last modified time will be used. 

2- The Reg images plugin is designed to co-register two images, one using a near-infrared camera and the other a “normal” visible camera. The plugin will work best if the images were acquired from two cameras mounted with their lenses close to each other, acquired at nearly the same time (so the scene hasn't changed), and it's best if the two cameras have similar characteristics such as image size and resolution. The plugin can output the following images:
- NGR image (false-color image with r=near-IR, g=green from visible, and r=red from visible)
- NDVI image with a user-selected color table applied
- Floating point NDVI image with actual NDVI values (data range -1 to +1)
- A visible image clipped to the common area between the registered near-IR and visible image
- A log file documenting the registration method used for each image pair

The .jar file which is the binary form of the plugin can be downloaded by clicking on the Downloads button on the PhotoMonitoringPlugin page on GitHub. Information about how to install and use the plugins is provided in the guide which can be downloaded form this GitHub site. 


Notes for developers:
The source files necesary to build these plugins are providd on this GitHub respository. The "com" folder contains the .class files from the Meatadata-Extractor project (http://www.drewnoakes.com/code/exif/). The license information and readme file for that project are in the "com" folder. These .class files are necessary to run the photo monitoring plugins. 


License:
The Photo Monitoring Plugin is an open source initiative and distributed free of charge with no warranty as stipulated in the GNU General Public License. See the GNU license page for more details: http://www.gnu.org/licenses/gpl.html.


To do list:
Add Javadoc comments to the code
Write unit test code 
Add options to modify input and output file names

Wish list:
Improve image matching algorithms
Add image matching/mosaciing capabilitiy
