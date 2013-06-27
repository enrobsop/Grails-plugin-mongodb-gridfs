package org.iglas.grails.gridfs

import org.iglas.grails.utils.*

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.gridfs.GridFSDBFile
import com.mongodb.gridfs.GridFSInputFile

class GridfsController {

	def configHelper = new ConfigHelper()  // for easier testing
	
	def gridfsService
	
    //messagesource
    def messageSource

    //defaultaction
    def defaultAction = "list"
    def index(){
        redirect(action: "help")
    }
    def list(params){
        GridfsService.list(params)
    }
	
	private def failBecauseIdParentMissing(config) {
		failIf(config, messageSource.getMessage("mongodb-gridfs.paramsbad", [params.idparent] as Object[], "Invalid params", request.locale)) {
			!params.idparent
		}
	}
	
	private def failBecauseFileMissing(file, config) {		
		failIf(config, messageSource.getMessage("mongodb-gridfs.upload.nofile", null, "No file was found with id {0}. Please check your link.", request.locale), [id: params.idparent]) {
			isEmptyFile(file)	
		}
	}

	private def failBecauseUnauthorizedFileExtension(file, config) {
		def fileExtension = file.originalFilename.substring(file.originalFilename.lastIndexOf('.')+1)
		failIf(config, messageSource.getMessage("mongodb-gridfs.upload.unauthorizedExtension", [fileExtension, config.allowedExtensions] as Object[], "The file you sent has an unauthorized extension ({0}). Allowed extensions for this upload are {1}", request.locale), [id: params.id]) {
			!config.allowedExtensions.contains(fileExtension)
		}
	}
	
	private def failBecauseFileTooBig(file, config) {
		def maxSizeInKb = (int) (config.maxSize ?: 0)/1024
		failIf(config, messageSource.getMessage("mongodb-gridfs.upload.fileBiggerThanAllowed", [maxSizeInKb] as Object[], "Sent file is bigger than allowed. Max file size is {0} kb", request.locale), [id: params.id]) {
			def limit = Math.max(config.maxSize ?: 0, 0) 
			limit && file.size > limit
		}
	}

	private def failIf(config, msg, params=[:], failureCondition) {
		if (failureCondition()) {
			log.debug msg
			flash.message = msg
			redirect controller:	config.controllers.errorController,
					action:			config.controllers.errorAction, 
					params:			params
			return true
		}
		false
	}
	
	private def failBecauseFileInvalid(file, config) {
		failBecauseFileMissing(file, config) || 
		failBecauseUnauthorizedFileExtension(file, config) ||
		failBecauseFileTooBig(file, config)
	}
	
	private def isEmptyFile(file) {
		!file || file.size == 0
	}

    def upload(params){

        def config = configHelper.getConfig(params)
		
        if (failBecauseIdParentMissing(config)) return
		
        def file = request.getFile("file")
		if (failBecauseFileInvalid(file, config)) return
		
        String newFileName = (params.idparent + "_" + file.originalFilename).toLowerCase()
        if (params?.parentclass)
        {
            newFileName = params.parentclass + "_" + newFileName
        }

        if(!gridfsService.exists(config, newFileName)){
			
            def gfsFile		= gridfsService.addToGridFS(config,file, newFileName.toLowerCase().replaceAll(/ /,""), params) 
			def checkUpload	= gridfsService.attemptUpload(config, gfsFile)
			
            if (checkUpload.isAllowed)
            {
				if (config.controllers.successType == "forward") {
					forward controller: config.controllers.successController, action: config.controllers.successAction
				} else {
                	redirect controller: config.controllers.successController, action: config.controllers.successAction
				}
            }
            else
            {
                log.debug "Access deny upload:" + checkUpload?.message
                flash.message = messageSource.getMessage("mongodb-gridfs.get.accessdeny", [access.message] as Object[], request.locale)
                redirect controller: config.accessController, action: config.accessAction
            }

        } else {
            log.debug "Filename for 'idparent'=${file.originalFilename} is busy"
            flash.message = messageSource.getMessage("mongodb-gridfs.upload.nameinbusy", [file.originalFilename] as Object[], request.locale)
            redirect controller: config.controllers.errorController, action: config.controllers.errorAction
        }


    }
    def get(params){
        if(params?.filename){
            def config = new UserConfig(GridfsService.configName).get(Gridfs.makeConfig(params))
            try {
                GridFSDBFile fileForOutput = GridfsService.get(params)

                if(fileForOutput)
                    if(fileForOutput){
                        def accessResult = true
                        if (config.accessClass && config.accessMethod ){
                            def access = Class.forName(config.accessClass  ,true,Thread.currentThread().contextClassLoader).newInstance()
                            accessResult = access."${config.accessMethod}"(fileForOutput,"get")
                        }

                        if (accessResult ){
                            response.outputStream << fileForOutput.getInputStream()
                            response.contentType = fileForOutput.getContentType()
                            return
                        }
                        else
                        {
                            log.debug "Access deny get:" + access.message
                            flash.message = messageSource.getMessage("mongodb-gridfs.get.accessdeny", [access.message] as Object[], request.locale)
                            redirect controller: config.accessController, action: config.accessAction, id: params.id
                        }


                    }
                    else
                    {
                        log.debug "File not found"
                        flash.message = messageSource.getMessage("mongodb-gridfs.get.filenotfound", [params.idparent] as Object[], request.locale)
                        redirect controller: config.controllers.errorController, action: config.controllers.errorAction, id: params.id
                    }
            } catch (e){
                def fileForOutput = false
            }

        }else{
            log.debug "Params  has errors"
            flash.message = messageSource.getMessage("mongodb-gridfs.paramsbad", [params.idparent] as Object[], request.locale)
            redirect controller: config.controllers.errorController, action: config.controllers.errorAction, id: params.id
        }

    }
    def remove(params){
        String rmFileName = params.filename
        def config = new UserConfig(GridfsService.configName).get(Gridfs.makeConfig(params))

        GridFSDBFile fileForRemove = GridfsService.get(filename:rmFileName)
        def accessResult = true
        if (config.accessClass && config.accessMethod ){
            def access = Class.forName(config.accessClass  ,true,Thread.currentThread().contextClassLoader).newInstance()
            accessResult = access."${config.accessMethod}"(fileForRemove,"remove")
        }

        if (accessResult)
        {
            GridfsService.remove(filename:rmFileName)
            redirect controller: config.controllers.successRemoveController, action: config.controllers.successRemoveAction
            return
        }
        else
        {
            log.debug "Access deny remove:" + access.message
            flash.message = messageSource.getMessage("mongodb-gridfs.get.accessdeny", [access.message] as Object[], request.locale)
            redirect controller: config.accessController, action: config.accessAction, id: params.id
        }

    }
    def help(){

    }

}
