

/**
 Constructor...
 */
VideoBucket = function(video, label) {
    
    this.video = video;
    this.label = label;
    
    this.transCanvas = null;
    this.transContext = null;
    this.coordinates = [];
    
    this.videoCanvas = MediaExt.createCanvas(640, 480);
    this.videoContext = this.videoCanvas.getContext("2d");
    
    this.transform = null; // new Geometry.PolyToCanvasTransform(poly, this.transCanvas);
}

/**
 Call this function after calibration
 @param poly ...
 @param rect The local shared rectangle
 */
VideoBucket.prototype.setTransform = function(poly, rect) {
    
    console.log('creating transform');
    console.log(poly);
    console.log(rect);
    
    this.coordinates = poly;
    this.transCanvas = MediaExt.createCanvas(rect.width, rect.height);
    this.transContext = this.transCanvas.getContext("2d");
    this.transform = new Geometry.PolyToCanvasTransform(poly, this.transCanvas);
}

VideoBucket.prototype.transformVideo = function() {
    
    if (this.transform == null) {
        return null;
    }
    
    var w = this.videoCanvas.width,
        h = this.videoCanvas.height;
    
    this.videoContext.drawImage(this.video, 0, 0, w, h);
    var imageData = this.videoContext.getImageData(0, 0, w, h);
    
    this.transform.transformImage(imageData, this.transCanvas);
    
    return this.transCanvas;
}

VideoBucket.transformList = function(bucketList) {
    var transformedVideos = [];
    for (var i = 0; i < bucketList.length; i++) {
        var tv = bucketList[i].transformVideo();
        if (tv != null) {
            transformedVideos.push(tv);
        }
    }
    return transformedVideos;
}

