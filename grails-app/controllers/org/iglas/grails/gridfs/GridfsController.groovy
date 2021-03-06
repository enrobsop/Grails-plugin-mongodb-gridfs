package org.iglas.grails.gridfs

import org.bson.types.ObjectId
import org.iglas.grails.utils.*

import com.mongodb.gridfs.GridFSDBFile

class GridfsController {

	def configHelper = new ConfigHelper()  // for easier testing
	def gridfsService
	def utilsService
    def messageSource
    def defaultAction = "list"
	
    def index(){
        redirect(action: "help")
    }
    def list(params){
        GridfsService.list(params)
    }
	
    def upload(UploadFileCommand command){

        def config = configHelper.getConfig(params)
		command.config = config
		
		// TODO switch to Grails Validation framework?
        if (failBecauseIdParentMissing(command) || failBecauseFileInvalid(command)) return
		
        if(!command.targetFileExists()){
			
			def checkUpload = command.createTargetFile()
			
            if (checkUpload.isAllowed)
            {
				switch(command.successType) {
					case "forward": 
						forward controller: command.successController, action: command.successAction, params: [fileId: command.targetFileId]
						break;
					case "chain":
						chain controller: command.successController, action: command.successAction
						break;
					default:
						redirect controller: command.successController, action: command.successAction
				}
            }
            else
            {
                log.debug "Access deny upload:" + checkUpload?.message
                flash.message = message("mongodb-gridfs.get.accessdeny", [access.message] as Object[], request.locale)
                redirect controller: config.accessController, action: config.accessAction
            }

        } else {
            log.debug "Filename for 'idparent'=${command.originalFilename} is busy"
            flash.message = message("mongodb-gridfs.upload.nameinbusy", [command.originalFilename] as Object[], request.locale)
            redirect controller: command.errorController, action: command.errorAction
        }

    }
    def get(GetFileCommand command){
    	def config = configHelper.getConfig(params)
        if(command.validate()){
			def filename = command.filename
            try {
                GridFSDBFile fileForOutput = gridfsService.getByFilename(filename)
                if (fileForOutput) {
                    def accessResult = true
					if (isPermitted("get", fileForOutput, config)) {
                        response.outputStream << fileForOutput.getInputStream()
                        response.contentType = fileForOutput.getContentType()
                        return
                    } else {
                        log.debug "Access denied for 'get'"
                        flash.message = messageSource.getMessage("mongodb-gridfs.get.accessdeny", ['get'] as Object[], "Access denied", request.locale)
                        redirect controller: config.accessController, action: config.accessAction, id: params.id
                    }
                } else {
					handleFileNotFound(config)
                }
            } catch (e) {
				e.printStackTrace()
				log.error e
				response.sendError(500)
				return
            }
        } else {
            flash.message = g.message(error: command.errors.fieldError)
			log.debug flash.message
            redirect controller: config.controllers.errorController, action: config.controllers.errorAction, id: params.id
        }
    }
	def showIcon() {
		def imgSrc
		if (params.id) {
			ObjectId oid = new ObjectId(params.id)
			imgSrc = utilsService.getIconForFile(oid,[width:params.width,height:params.height])			
		} else if (params.filename) {
			imgSrc = utilsService.getIconForFile(attrs.filename,[width:params.width,height:params.height])
		}
		
		def icon = imgSrc?.icon
		
		response.contentType = "image/jpg"
		response.outputStream << new FileInputStream(icon)
	}
	private boolean isPermitted(action, resource, config) {
		if (config.accessClass && config.accessMethod ){
			def access = Class.forName(config.accessClass  ,true,Thread.currentThread().contextClassLoader).newInstance()
			return access."${config.accessMethod}"(resource, action)
		}
		true
	}
	private void handleFileNotFound(config) {
		log.debug "File not found"
		flash.message = messageSource.getMessage("mongodb-gridfs.get.filenotfound", [params.filename] as Object[], "The requested file ({0}) was not found in our system.", request.locale)
		redirect controller: config.controllers.errorController, action: config.controllers.errorAction, id: params.id
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

	private def failBecauseIdParentMissing(command) {
		failIf(command, 
			message("mongodb-gridfs.paramsbad", [command.idparent] as Object[], "Invalid params")) {
			!command.idparent
		}
	}
	
	private def failBecauseFileMissing(command) {
		failIf(command, message("mongodb-gridfs.upload.nofile", null, "No file was found with id {0}. Please check your link."), [id: command.idparent]) {
			command.isFileEmpty()
		}
	}

	private def failBecauseUnauthorizedFileExtension(command) {
		def config = command.config
		def fileExtension = command.fileExtension
		failIf(command, message("mongodb-gridfs.upload.unauthorizedExtension", [fileExtension, command.allowedExtensions] as Object[], "The file you sent has an unauthorized extension ({0}). Allowed extensions for this upload are {1}"), [id: command.id]) {
			!command.isExtensionAllowed()
		}
	}
	
	private def failBecauseFileTooBig(command) {
		def maxSizeInKb = (int) (command.maxSize ?: 0)/1024
		failIf(command, message("mongodb-gridfs.upload.fileBiggerThanAllowed", [maxSizeInKb] as Object[], "Sent file is bigger than allowed. Max file size is {0} kb"), [id: command.id]) {
			command.isFileTooBig()
		}
	}

	private def failIf(command, msg, params=[:], failureCondition) {
		if (failureCondition()) {
			log.debug msg
			flash.message = msg
			redirect controller:	command.errorController,
					action:			command.errorAction,
					params:			params
			return true
		}
		false
	}
	
	private def failBecauseFileInvalid(command) {
		failBecauseFileMissing(command) ||
		failBecauseUnauthorizedFileExtension(command) ||
		failBecauseFileTooBig(command)
	}
	
	private def message(code, args, defaultMsg, locale=request.locale) {
		messageSource.getMessage(code, args, defaultMsg, locale)
	}
	
}
