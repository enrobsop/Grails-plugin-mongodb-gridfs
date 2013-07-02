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
	String getIconForFile(ObjectId oid, def params=[:]) {
		getIconForFile(gridfsService.getById(oid),params)
	}
    String getIconForFile(String filename,def params=[:]){
        getIconForFile(GridfsService.get(filename: filename),params)
    }
    public String getIconForFile(GridFSFile file,def params=[:]){
    	def config = configHelper.getConfig(params)
        String iconDir = config.iconsdir.toLowerCase()
        List imagesType = config.imagestype
        Map thumbConfig = config.thumbconfig

        String prefix  = ""
        if(new File("web-app/").isDirectory())
            prefix  =  "web-app/"

        DBObject metadata = file.getMetaData()
        String idparent = metadata.idparent
        String thumbdir = thumbConfig.publicdir.toLowerCase()
        thumbdir = thumbdir.replaceAll(/\[idparent\]/,idparent)
        new File(prefix + thumbdir).mkdirs()
        String icon  = grailsLinkGenerator.resource(file: config.defaulticon)

        if(metadata?.fileExtension?.toLowerCase() in imagesType){

            if(!new File(prefix + thumbdir +"/"+metadata.originalFilename.toLowerCase()).isFile())
            {
                String tmpfile  = config.tmpdir + "/" + file.filename

                new File(prefix + config.tmpdir).mkdirs()
                GridFSDBFile fileForTmp
                fileForTmp = gridfsService.getByFilename(file.filename)

                if(fileForTmp !=  null) {
                    if(!new File(prefix + tmpfile).isFile())
                        fileForTmp.writeTo(prefix + tmpfile)
                    String filenameForBurn =thumbConfig.x_size +"x" + thumbConfig.y_size + metadata.originalFilename.substring(0,metadata.originalFilename.lastIndexOf('.'))
                    burningImageService.doWith(prefix + tmpfile,prefix + thumbdir)
                            .execute (filenameForBurn.toLowerCase(), {
                        it.scaleAccurate(thumbConfig.x_size, thumbConfig.y_size)
                    })

                }
            }
            icon  = grailsLinkGenerator.resource([dir:  thumbdir , file: thumbConfig.x_size +"x" + thumbConfig.y_size + metadata.originalFilename.toLowerCase()])
        }else {
            def iconFile = getFilePathForExtension(iconDir,metadata.fileExtension.toLowerCase())
            if (iconFile)
                icon = grailsLinkGenerator.resource([dir: iconDir , file: iconFile])
        }
        icon
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
