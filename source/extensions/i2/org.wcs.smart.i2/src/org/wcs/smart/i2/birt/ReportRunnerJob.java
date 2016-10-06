package org.wcs.smart.i2.birt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.birt.entity.EntityParameterMetadata.EntityParameter;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

public class ReportRunnerJob extends Job {

	private IntelEntity entity;
	
	public ReportRunnerJob(IntelEntity entity) {
		super("Running BIRT Report");
		this.entity = entity;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		
		if (entity.getEntityType().getBirtTemplate() == null){
			//TODO: provide error message to user
			return Status.OK_STATUS;
		}
		Path reportFile = IntelReportManager.INSTANCE.getEntityTemplate(entity.getEntityType());
		
		Path outputFile = IntelReportManager.INSTANCE.getTemporaryDirectory();
		if (!Files.exists(outputFile)){
			try {
				Files.createDirectory(outputFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		outputFile = outputFile.resolve(UuidUtils.uuidToString(entity.getUuid()) + "." + System.nanoTime() + ".pdf");
		
		IRenderOption options = new RenderOption();
		try(FileOutputStream fout = new FileOutputStream(outputFile.toFile())){
			
			options.setOutputStream(fout);
			options.setEmitterID("org.eclipse.birt.report.engine.emitter.pdf");
			options.setOption(HTMLRenderOption.IMAGE_DIRECTROY, outputFile.getParent());
			options.setSupportedImageFormats("PNG"); //$NON-NLS-1$
			
			HashMap<String, Object> reportParameters = new HashMap<String, Object>();
			reportParameters.put(EntityParameter.UUID.name, UuidUtils.uuidToString(entity.getUuid()));
			Session session = HibernateManager.openSession();
			try{
				IntelReportRunner.INSTANCE.runFile(reportFile.toFile(), 
						SmartDB.getCurrentConservationArea(),
						SmartLabelProvider.getShortLabel(SmartDB.getCurrentEmployee()),
						ReportEngineManager.getBirtReportEngine(),
						options,
						session,
						reportParameters);	
			}finally{
				if (session.isOpen()) session.close();
			}
		}catch (Exception ex){
			//TODO:
			ex.printStackTrace();
		}
		
		final File out = outputFile.toFile();
		Display.getDefault().syncExec(() ->  AttachmentUtil.launch(out));					
		return Status.OK_STATUS;
	}

}
