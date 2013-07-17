package org.iglas.grails.gridfs

import org.bson.types.ObjectId
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.iglas.grails.utils.*

import pl.burningice.plugins.image.BurningImageService

import com.mongodb.DBObject
import com.mongodb.gridfs.GridFSDBFile
import com.mongodb.gridfs.GridFSFile

class UtilsService {
    // save last query total result
    static Integer total
    LinkGenerator  grailsLinkGenerator
	def configHelper = new ConfigHelper()  // for easier testing
	
	def grailsApplication
	def gridfsService
	def burningImageService
	
    UtilsService(){
        grailsLinkGenerator = grailsApplication?.mainContext?.getBean("grailsLinkGenerator")
    }
	def getIconForFile(ObjectId oid, def params=[:]) {
		getIconForFile(gridfsService.getById(oid),params)
	}
    def getIconForFile(String filename,def params=[:]){
        getIconForFile(GridfsService.get(filename: filename),params)
    }
    public def getIconForFile(GridFSFile file,def params=[:]){
    	def config = configHelper.getConfig(params)
		
    	def iconProperties
		def icon
		
        DBObject metadata 		= file.getMetaData()
		def fileExtension 		= metadata.fileExtension.toLowerCase()
		def originalFilename	= metadata.originalFilename.toLowerCase()
		def idparent 			= metadata.idparent
		
		Map thumbConfig		= config.thumbconfig
		def thumbWidth		= (params.width  ?: thumbConfig.x_size) as int
		def thumbHeight		= (params.height ?: thumbConfig.y_size) as int
		def thumbFilename	= getThumbnailName(originalFilename, thumbWidth, thumbHeight)
		
		def publicPath	= thumbConfig.publicdir.toLowerCase().replaceAll(/\[idparent\]/,idparent)
		def tmpDir 		= initDir(config.tmpdir)	
		def thumbDir 	= initDir(publicPath)
		
        if(isImageType(fileExtension, config)){
			initThumbnail(file, tmpDir, thumbDir, thumbFilename, thumbWidth, thumbHeight)
			iconProperties	= [dir:  publicPath , file: "$thumbFilename.$fileExtension"]
			icon			= new File(thumbDir, "$thumbFilename.$fileExtension")
        } else {
			iconProperties	= getIconFor(fileExtension, config)
        }

		iconProperties = iconProperties ?: [file: config.defaulticon]
		[
			link:	grailsLinkGenerator.resource(iconProperties),
			icon:	icon ?: new File(iconProperties.dir as String, iconProperties.file as String)
		]
        
    }
	
	private void initThumbnail(file, tmpDir, thumbDir, thumbFilename, width, height) {
		if(!new File(thumbDir, thumbFilename).isFile()) {
			def srcImage = materializeToFilesystem(file, tmpDir).path
			makeThumbnail(srcImage, thumbDir, thumbFilename, width, height);
		}
	}
	
	private def getIconFor(fileExtension, config) {
		String iconDir	= config.iconsdir.toLowerCase()
		def iconFile	= getFilePathForExtension(iconDir,fileExtension)
		[dir: iconDir , file: iconFile]
	}
	
	def getPrefix() {
		new File("web-app/").isDirectory() ? "web-app/" : ""
	}
	
	def initDir(path) {
		def dir = new File(prefix + path) 
		dir.mkdirs()
		dir.path
	}
	
	private makeThumbnail(inputImagePath, thumbDirPath, outputFilename, width, height) {
		burningImageService.doWith(inputImagePath, thumbDirPath).execute(outputFilename, {
			it.scaleAccurate(width, height)
		})
	}
	
	private boolean isImageType(extension, config) {
		extension in config.imagestype
	}
	
	private def materializeToFilesystem(GridFSDBFile dbFile, String dirPath) {
		def materializedFile = new File(dirPath, dbFile.filename)
		if(!materializedFile.isFile()) {
			dbFile.writeTo(materializedFile)
		}
		materializedFile
	}
	
	def getThumbnailName(String originalFilename, width, height) {
		def cleaned = originalFilename.trim().toLowerCase()  
		"${width}x${height}" + cleaned.substring(0,cleaned.lastIndexOf('.'))
	}
	
    static getFilePathForExtension(String dir,String extension){

        def iconConfigFile = new File(dir + "/iconconfig.groovy")
        if(iconConfigFile.isFile()){
            def iconConfig  = new ConfigSlurper().parse(iconConfigFile.toURL())
            if(iconConfig?.iconsOfExtension instanceof Map && iconConfig?.iconsOfExtension[extension]){
                return  iconConfig.iconsOfExtension[extension]
            }
        }
        false
    }
    static deleteIcons(GridFSFile file){
        def config = new UserConfig(GridfsService.configName).get()
        String prefix  = ""
        if(new File("web-app/").isDirectory())
            prefix  =  "web-app/"
        String thumbdir = config.thumbconfig.publicdir.toLowerCase()
        thumbdir = thumbdir.replaceAll(/\[idparent\]/,file.getMetaData().idparent)
        new File(prefix + thumbdir).eachFileMatch( ~".*${file.getMetaData().originalFilename.toLowerCase()}" ) { f ->
            f.delete()
        }
    }
}
