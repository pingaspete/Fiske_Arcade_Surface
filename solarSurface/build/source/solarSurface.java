import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.net.*; 
import java.util.Arrays; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class solarSurface extends PApplet {

String dataDir    = "../data/";
OPC opc;

PImage solarSurface;
PImage spotColor;

int canvasWidth   = 55; //// 55
int canvasHeight  = 91; //// 19
int maxNumFeatures = 100;

float noiseIncrement = 0.25f;  /// CHANGE SIZE OF GRANUAL
float zoff = 0.0f;
float xoffStart = 90.0f;

OpenSimplexNoise noise;

// PImage plageColor;

ArrayList<SolarFeature> solarFeatures;

PVector[] arches = new PVector[12];


public void settings(){
  size(canvasWidth, canvasHeight);
}

public void setup(){
  // blendMode(DARKEST);  //// FOR BLENDING OF THE FEATURES WITH THE SURFACE
  // frameRate(5);

  opc = new OPC(this, "127.0.0.1", 7890);  // Connect to the local instance of fcserver
  opc.ledGrid(0, 54, 90, width/2, height/2, width/width, height/height, 0, false, false); // Create LED Grid

  noStroke();

  noise         = new OpenSimplexNoise();
  solarFeatures = new ArrayList<SolarFeature>();
  solarSurface  = loadImage(dataDir + "solarSurface.png");
  spotColor     = loadImage(dataDir + "sunspot.png");
  // plageColor    = loadImage(dataDir + "plage.png");


  ///////////// HARD CODE POS OF ARCHES /////////////
  // arches[0] = new PVector(0, 0);
  // arches[1] = new PVector(0, 55);
  //
  // arches[2] = new PVector(18, 0);
  // arches[3] = new PVector(18,55);
  //
  // arches[4] = new PVector(36, 0);
  // arches[5] = new PVector(36,55);
  //
  // arches[6] = new PVector(54, 0);
  // arches[7] = new PVector(54,55);
  //
  // arches[8] = new PVector(72, 0);
  // arches[9] = new PVector(72,55);
  //
  // arches[10] = new PVector(91, 0);
  // arches[11] = new PVector(91,55);
  ///////////////////////////////////


  ///////////// HARD CODE POS OF ARCHES /////////////
  // arches[0] = new PVector(0, 0);
  // arches[1] = new PVector(55, 0);
  //
  // arches[2] = new PVector(0, 18);
  // arches[3] = new PVector(55, 18);
  //
  // arches[4] = new PVector(0, 36);
  // arches[5] = new PVector(55, 36);
  //
  // arches[6] = new PVector(0, 54);
  // arches[7] = new PVector(55, 54);
  //
  // arches[8] = new PVector(0, 72);
  // arches[9] = new PVector(55, 72);
  //
  // arches[10] = new PVector(0, 91);
  // arches[11] = new PVector(55, 91);
  ///////////////////////////////////


  arches[0] = new PVector(0, 0);
  arches[1] = new PVector(width, 0);

  arches[2] = new PVector(0, height*0.18f);
  arches[3] = new PVector(width, height*0.18f);

  arches[4] = new PVector(0, height*0.36f);
  arches[5] = new PVector(width, height*0.36f);

  arches[6] = new PVector(0, height*0.54f);
  arches[7] = new PVector(width, height*0.54f);

  arches[8] = new PVector(0, height*0.72f);
  arches[9] = new PVector(width, height*0.72f);

  arches[10] = new PVector(0, height);
  arches[11] = new PVector(width, height);



}

public void draw() {
  drawSurface();
  drawFeatures();
}


public void mousePressed(){
    if (solarFeatures.size() < maxNumFeatures){
      //// 0 == SPOT  /   1 == PLAGE
      // solarFeatures.add(new SolarFeature(mouseX, mouseY, 0));
      solarFeatures.add(new SolarFeature(mouseX, mouseY, 0));
  }
}


public void drawFeatures(){
  if (solarFeatures.size() > 0){
    for (int i = 0; i < solarFeatures.size(); i++){

    if (solarFeatures.get(i).type == 0) {
      solarFeatures.get(i).show();
      solarFeatures.get(i).update();

    }
    // else if (solarFeatures.get(i).type == 1) {
    //
    // }
    }

    for (int i = 0; i < solarFeatures.size(); i++){

      if (solarFeatures.get(i).currentSize < 0){
        solarFeatures.remove(i);  //// REMOVE OLDER SUN SPOTS
      }
  }

  }



}


public void drawSurface(){
  loadPixels();
  float xoff = xoffStart;
  xoffStart -= 0.01f;              /// ADJUST SPEED OF LEFT-RIGHT FLOW
  zoff += 0.05f;                  /// ADJUST SPEED OF 3D FLOW

  for (int x = 0; x < width; x++) {
    xoff += noiseIncrement;
    float yoff = 0.0f;
    for (int y = 0; y < height; y++) {
      yoff += noiseIncrement;

      float n = (float) noise.eval(xoff, yoff, zoff);

      int colorPos;
      PImage colorField;

      // if (dist(x,y, x, height/2) < 10){
      //   colorField = plageColor;
      // }
      // else{
        colorField = solarSurface;
      // }

      colorPos = round(map(n, -1, 1, 0, colorField.width));
      pixels[x+y*width] = color(colorField.get(colorPos, 0));


    }
  }
  updatePixels();
}
/*
 * Simple Open Pixel Control client for Processing,
 * designed to sample each LED's color from some point on the canvas.
 *
 * Micah Elizabeth Scott, 2013
 * This file is released into the public domain.
 */




public class OPC implements Runnable
{
  Thread thread;
  Socket socket;
  OutputStream output, pending;
  String host;
  int port;

  int[] pixelLocations;
  byte[] packetData;
  byte firmwareConfig;
  String colorCorrection;
  boolean enableShowLocations;

  OPC(PApplet parent, String host, int port)
  {
    this.host = host;
    this.port = port;
    thread = new Thread(this);
    thread.start();
    this.enableShowLocations = true;
    parent.registerMethod("draw", this);
  }

  // Set the location of a single LED
  public void led(int index, int x, int y)  
  {
    // For convenience, automatically grow the pixelLocations array. We do want this to be an array,
    // instead of a HashMap, to keep draw() as fast as it can be.
    if (pixelLocations == null) {
      pixelLocations = new int[index + 1];
    } else if (index >= pixelLocations.length) {
      pixelLocations = Arrays.copyOf(pixelLocations, index + 1);
    }

    pixelLocations[index] = x + width * y;
  }
  
  // Set the location of several LEDs arranged in a strip.
  // Angle is in radians, measured clockwise from +X.
  // (x,y) is the center of the strip.
  public void ledStrip(int index, int count, float x, float y, float spacing, float angle, boolean reversed)
  {
    float s = sin(angle);
    float c = cos(angle);
    for (int i = 0; i < count; i++) {
      led(reversed ? (index + count - 1 - i) : (index + i),
        (int)(x + (i - (count-1)/2.0f) * spacing * c + 0.5f),
        (int)(y + (i - (count-1)/2.0f) * spacing * s + 0.5f));
    }
  }

  // Set the locations of a ring of LEDs. The center of the ring is at (x, y),
  // with "radius" pixels between the center and each LED. The first LED is at
  // the indicated angle, in radians, measured clockwise from +X.
  public void ledRing(int index, int count, float x, float y, float radius, float angle)
  {
    for (int i = 0; i < count; i++) {
      float a = angle + i * 2 * PI / count;
      led(index + i, (int)(x - radius * cos(a) + 0.5f),
        (int)(y - radius * sin(a) + 0.5f));
    }
  }

  // Set the location of several LEDs arranged in a grid. The first strip is
  // at 'angle', measured in radians clockwise from +X.
  // (x,y) is the center of the grid.
  public void ledGrid(int index, int stripLength, int numStrips, float x, float y,
               float ledSpacing, float stripSpacing, float angle, boolean zigzag,
               boolean flip)
  {
    float s = sin(angle + HALF_PI);
    float c = cos(angle + HALF_PI);
    for (int i = 0; i < numStrips; i++) {
      ledStrip(index + stripLength * i, stripLength,
        x + (i - (numStrips-1)/2.0f) * stripSpacing * c,
        y + (i - (numStrips-1)/2.0f) * stripSpacing * s, ledSpacing,
        angle, zigzag && ((i % 2) == 1) != flip);
    }
  }

  // Set the location of 64 LEDs arranged in a uniform 8x8 grid.
  // (x,y) is the center of the grid.
  public void ledGrid8x8(int index, float x, float y, float spacing, float angle, boolean zigzag,
                  boolean flip)
  {
    ledGrid(index, 8, 8, x, y, spacing, spacing, angle, zigzag, flip);
  }

  // Should the pixel sampling locations be visible? This helps with debugging.
  // Showing locations is enabled by default. You might need to disable it if our drawing
  // is interfering with your processing sketch, or if you'd simply like the screen to be
  // less cluttered.
  public void showLocations(boolean enabled)
  {
    enableShowLocations = enabled;
  }
  
  // Enable or disable dithering. Dithering avoids the "stair-stepping" artifact and increases color
  // resolution by quickly jittering between adjacent 8-bit brightness levels about 400 times a second.
  // Dithering is on by default.
  public void setDithering(boolean enabled)
  {
    if (enabled)
      firmwareConfig &= ~0x01;
    else
      firmwareConfig |= 0x01;
    sendFirmwareConfigPacket();
  }

  // Enable or disable frame interpolation. Interpolation automatically blends between consecutive frames
  // in hardware, and it does so with 16-bit per channel resolution. Combined with dithering, this helps make
  // fades very smooth. Interpolation is on by default.
  public void setInterpolation(boolean enabled)
  {
    if (enabled)
      firmwareConfig &= ~0x02;
    else
      firmwareConfig |= 0x02;
    sendFirmwareConfigPacket();
  }

  // Put the Fadecandy onboard LED under automatic control. It blinks any time the firmware processes a packet.
  // This is the default configuration for the LED.
  public void statusLedAuto()
  {
    firmwareConfig &= 0x0C;
    sendFirmwareConfigPacket();
  }    

  // Manually turn the Fadecandy onboard LED on or off. This disables automatic LED control.
  public void setStatusLed(boolean on)
  {
    firmwareConfig |= 0x04;   // Manual LED control
    if (on)
      firmwareConfig |= 0x08;
    else
      firmwareConfig &= ~0x08;
    sendFirmwareConfigPacket();
  } 

  // Set the color correction parameters
  public void setColorCorrection(float gamma, float red, float green, float blue)
  {
    colorCorrection = "{ \"gamma\": " + gamma + ", \"whitepoint\": [" + red + "," + green + "," + blue + "]}";
    sendColorCorrectionPacket();
  }
  
  // Set custom color correction parameters from a string
  public void setColorCorrection(String s)
  {
    colorCorrection = s;
    sendColorCorrectionPacket();
  }

  // Send a packet with the current firmware configuration settings
  public void sendFirmwareConfigPacket()
  {
    if (pending == null) {
      // We'll do this when we reconnect
      return;
    }
 
    byte[] packet = new byte[9];
    packet[0] = (byte)0x00; // Channel (reserved)
    packet[1] = (byte)0xFF; // Command (System Exclusive)
    packet[2] = (byte)0x00; // Length high byte
    packet[3] = (byte)0x05; // Length low byte
    packet[4] = (byte)0x00; // System ID high byte
    packet[5] = (byte)0x01; // System ID low byte
    packet[6] = (byte)0x00; // Command ID high byte
    packet[7] = (byte)0x02; // Command ID low byte
    packet[8] = (byte)firmwareConfig;

    try {
      pending.write(packet);
    } catch (Exception e) {
      dispose();
    }
  }

  // Send a packet with the current color correction settings
  public void sendColorCorrectionPacket()
  {
    if (colorCorrection == null) {
      // No color correction defined
      return;
    }
    if (pending == null) {
      // We'll do this when we reconnect
      return;
    }

    byte[] content = colorCorrection.getBytes();
    int packetLen = content.length + 4;
    byte[] header = new byte[8];
    header[0] = (byte)0x00;               // Channel (reserved)
    header[1] = (byte)0xFF;               // Command (System Exclusive)
    header[2] = (byte)(packetLen >> 8);   // Length high byte
    header[3] = (byte)(packetLen & 0xFF); // Length low byte
    header[4] = (byte)0x00;               // System ID high byte
    header[5] = (byte)0x01;               // System ID low byte
    header[6] = (byte)0x00;               // Command ID high byte
    header[7] = (byte)0x01;               // Command ID low byte

    try {
      pending.write(header);
      pending.write(content);
    } catch (Exception e) {
      dispose();
    }
  }

  // Automatically called at the end of each draw().
  // This handles the automatic Pixel to LED mapping.
  // If you aren't using that mapping, this function has no effect.
  // In that case, you can call setPixelCount(), setPixel(), and writePixels()
  // separately.
  public void draw()
  {
    if (pixelLocations == null) {
      // No pixels defined yet
      return;
    }
    if (output == null) {
      return;
    }

    int numPixels = pixelLocations.length;
    int ledAddress = 4;

    setPixelCount(numPixels);
    loadPixels();

    for (int i = 0; i < numPixels; i++) {
      int pixelLocation = pixelLocations[i];
      int pixel = pixels[pixelLocation];

      packetData[ledAddress] = (byte)(pixel >> 16);
      packetData[ledAddress + 1] = (byte)(pixel >> 8);
      packetData[ledAddress + 2] = (byte)pixel;
      ledAddress += 3;

      if (enableShowLocations) {
        pixels[pixelLocation] = 0xFFFFFF ^ pixel;
      }
    }

    writePixels();

    if (enableShowLocations) {
      updatePixels();
    }
  }
  
  // Change the number of pixels in our output packet.
  // This is normally not needed; the output packet is automatically sized
  // by draw() and by setPixel().
  public void setPixelCount(int numPixels)
  {
    int numBytes = 3 * numPixels;
    int packetLen = 4 + numBytes;
    if (packetData == null || packetData.length != packetLen) {
      // Set up our packet buffer
      packetData = new byte[packetLen];
      packetData[0] = (byte)0x00;              // Channel
      packetData[1] = (byte)0x00;              // Command (Set pixel colors)
      packetData[2] = (byte)(numBytes >> 8);   // Length high byte
      packetData[3] = (byte)(numBytes & 0xFF); // Length low byte
    }
  }
  
  // Directly manipulate a pixel in the output buffer. This isn't needed
  // for pixels that are mapped to the screen.
  public void setPixel(int number, int c)
  {
    int offset = 4 + number * 3;
    if (packetData == null || packetData.length < offset + 3) {
      setPixelCount(number + 1);
    }

    packetData[offset] = (byte) (c >> 16);
    packetData[offset + 1] = (byte) (c >> 8);
    packetData[offset + 2] = (byte) c;
  }
  
  // Read a pixel from the output buffer. If the pixel was mapped to the display,
  // this returns the value we captured on the previous frame.
  public int getPixel(int number)
  {
    int offset = 4 + number * 3;
    if (packetData == null || packetData.length < offset + 3) {
      return 0;
    }
    return (packetData[offset] << 16) | (packetData[offset + 1] << 8) | packetData[offset + 2];
  }

  // Transmit our current buffer of pixel values to the OPC server. This is handled
  // automatically in draw() if any pixels are mapped to the screen, but if you haven't
  // mapped any pixels to the screen you'll want to call this directly.
  public void writePixels()
  {
    if (packetData == null || packetData.length == 0) {
      // No pixel buffer
      return;
    }
    if (output == null) {
      return;
    }

    try {
      output.write(packetData);
    } catch (Exception e) {
      dispose();
    }
  }

  public void dispose()
  {
    // Destroy the socket. Called internally when we've disconnected.
    // (Thread continues to run)
    if (output != null) {
      println("Disconnected from OPC server");
    }
    socket = null;
    output = pending = null;
  }

  public void run()
  {
    // Thread tests server connection periodically, attempts reconnection.
    // Important for OPC arrays; faster startup, client continues
    // to run smoothly when mobile servers go in and out of range.
    for(;;) {

      if(output == null) { // No OPC connection?
        try {              // Make one!
          socket = new Socket(host, port);
          socket.setTcpNoDelay(true);
          pending = socket.getOutputStream(); // Avoid race condition...
          println("Connected to OPC server");
          sendColorCorrectionPacket();        // These write to 'pending'
          sendFirmwareConfigPacket();         // rather than 'output' before
          output = pending;                   // rest of code given access.
          // pending not set null, more config packets are OK!
        } catch (ConnectException e) {
          dispose();
        } catch (IOException e) {
          dispose();
        }
      }

      // Pause thread to avoid massive CPU load
      try {
        Thread.sleep(500);
      }
      catch(InterruptedException e) {
      }
    }
  }
}
class SolarFeature{
  PShape  outsideShape;
  PShape  insideShape;

  PVector pos = new PVector(0, 0);
  PVector vel = new PVector(0, 0);
  PVector acc = new PVector(0, 0);

  int     destArch = round(random(0, 11));

  int     type;
  int     age = 0;                          //// CURRENT LIFETIME OF THIS FEATURE (IN FPS)
  int     outsideOpacity = 64;
  int     insideOpacity = 200;

  float   lifeSpan = 5 * frameRate;       //// NUM OF SECONDS * FPS TO KEEP FEATURE AT MAX OPACITY (ASSUMING ~60FPS)
  float   currentSize = 0;
  float   maxSize = random(canvasWidth*0.01f, canvasWidth*0.2f);  //// MAX SIZE OF FEATURE (10% OF CANVAS WIDTH)
  float   perlinSeed = random(100);
  float   increment;
  float   flux = 0.75f;

  boolean maxed = false;

  int   featureColor;



  SolarFeature(int x, int y, int type){
    pos.x = x;
    pos.y = y;


    if (type == 0){
      makeSpotShape();
    }

  }

  public void show(){
    showSpots();
  }

  public void makeSpotShape(){
    perlinSeed += 0.025f;                                  //// SPEED OF FLUX WITHIN FEATURE
    outsideShape = createShape();
    outsideShape.beginShape();
    insideShape = createShape();
    insideShape.beginShape();
    increment = random(maxSize*0.001f, maxSize*0.03f);

    for (float v = 0; v < TWO_PI; v+=0.025f){
        float xoff = map(cos(v)+perlinSeed, -1, 1, 1, 3); //// JAGGEDNESS OF EDGES (HIGHER = MORE JAGGED)
        float yoff = map(sin(v)+perlinSeed, -1, 1, 1, 3); //// JAGGEDNESS OF EDGES (HIGHER = MORE JAGGED)

        //// MOVING THROUGH NOISE SPACE:
        float n = map((float) (noise .eval(xoff, yoff)), -1, 1, 0, 1);
        float r = map(n, -1, 1, currentSize - flux, currentSize);

        float vX = r * cos(v);
        float vY = r * sin(v);

        outsideShape.vertex(vX, vY);
        insideShape.vertex(vX, vY);

    }

    outsideShape.endShape(CLOSE);
    insideShape.endShape(CLOSE);
    insideShape.scale(0.5f);

    outsideShape.setFill(color(46, 1, 4, outsideOpacity));
    insideShape.setFill(color(46, 1, 4, insideOpacity));

  }



  public void showSpots(){
    // pushMatrix();
      // translate(pos.x, pos.y);  //// PLACE UNDER MOUSE (OR WHEREVER pos.x/pos.y HAS BEEN SET)
      shape(outsideShape, pos.x, pos.y);
      shape(insideShape, pos.x, pos.y);
    // popMatrix();



  }

  int count = 0;

  public void moveSpot(){
    //


    //   ///// MOVE TOWARD DEST
    PVector dest = new PVector();
    dest = arches[destArch].copy();
    if (dist(pos.x, pos.y, dest.x, dest.y) < 10){

      println(count++);
      // acc.add(PVector.random2D().mult(0.075));

    }


    dest.sub(pos);
    dest.setMag(0.0001f);
    acc = dest;

    // vel = new PVector(0, 0);
    // acc = new PVector(0, 0);
    acc.add(PVector.random2D().mult(0.005f));
    vel.add(acc);
    pos.add(vel);





  }


  public void update(){
    makeSpotShape(); //// MAKE NEW SHAPE WITH SLIGHTLY DIFFERENT VERTS (CREATES THE FLUX EFFECT)
    moveSpot();

    if (currentSize < maxSize && maxed == false){
      currentSize += increment;
    }
    else{
      maxed = true;
    }

    if(maxed){
      age += 1;
    }

    if (age > lifeSpan){
      currentSize -= increment;
    }




  }
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "solarSurface" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
