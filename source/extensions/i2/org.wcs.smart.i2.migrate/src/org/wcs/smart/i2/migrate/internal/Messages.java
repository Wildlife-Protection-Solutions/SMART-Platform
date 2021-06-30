/*
 * Copyright (C) 2021 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.i2.migrate.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.i2.migrate.internal.messages"; //$NON-NLS-1$
	public static String CaListWizardPage_Message;
	public static String CaListWizardPage_Title;
	public static String ConversionJob_CompleteMsg;
	public static String ConversionJob_CompleteTitle;
	public static String ConversionJob_ProfileRecordComment;
	public static String ConversionJob_TaskName;
	public static String EntityMigrationJob_ActiveName;
	public static String EntityMigrationJob_ContinueButton;
	public static String EntityMigrationJob_DuplicateAttributes;
	public static String EntityMigrationJob_EntityMigrationComment;
	public static String EntityMigrationJob_ErrorAttributeNotFound;
	public static String EntityMigrationJob_ErrorDataModelAttributeNotFound;
	public static String EntityMigrationJob_ErrorListItemNotFound;
	public static String EntityMigrationJob_ErrorListItemNotFound2;
	public static String EntityMigrationJob_ErrorMatchingEntityNotFound;
	public static String EntityMigrationJob_ErrorRelatinshipNotFound;
	public static String EntityMigrationJob_ErrorTreeMultiListAttributesNotSupported;
	public static String EntityMigrationJob_IDName;
	public static String EntityMigrationJob_InactiveName;
	public static String EntityMigrationJob_PositionName;
	public static String EntityMigrationJob_PrimaryAttributeGroupName;
	public static String EntityMigrationJob_StatusName;
	public static String EntityMigrationJob_SubTask;
	public static String EntityMigrationJob_TaskName;
	public static String EntityMigrationJob_templatesubtask;
	public static String EntityMigrationJob_WarningListItemAdded;
	public static String EntityMigrationJob_WarningMsg;
	public static String EntityMigrationJob_WarningTitle;
	public static String EntityTypeMappingPage_CaColumnTitle;
	public static String EntityTypeMappingPage_CaProfileName;
	public static String EntityTypeMappingPage_CaTypeName;
	public static String EntityTypeMappingPage_Message;
	public static String EntityTypeMappingPage_Title;
	public static String ExtractDbJob_FileNotFound;
	public static String ExtractDbJob_invalidVersion;
	public static String ExtractDbJob_subtask1;
	public static String ExtractDbJob_subtask2;
	public static String ExtractDbJob_subtask3;
	public static String ExtractDbJob_TaskName;
	public static String MigrateEntityWizard_CompleteMessage;
	public static String MigrateEntityWizard_CompleteTitle;
	public static String MigrateEntityWizard_ConversionError;
	public static String MigrateIntelligenceWizard_NoCas;
	public static String MigrateIntelligenceWizard_NoMatchingCas;
	public static String MigrateIntelligenceWizard_Title;
	public static String RecordMappingPage_CaHeader;
	public static String RecordMappingPage_CannotMapToSameAttribute;
	public static String RecordMappingPage_DoNotImportOption;
	public static String RecordMappingPage_FromDateHeader;
	public static String RecordMappingPage_Message;
	public static String RecordMappingPage_ProfileHeader;
	public static String RecordMappingPage_RecordHeader;
	public static String RecordMappingPage_SrcHeader;
	public static String RecordMappingPage_Title;
	public static String RecordMappingPage_ToDateHeader;
	public static String Smart6WizardPage_AllFiles;
	public static String Smart6WizardPage_lblFileName;
	public static String Smart6WizardPage_Message;
	public static String Smart6WizardPage_Title;
	public static String Smart6WizardPage_ZipFiles;
	public static String ValidateUserJob_AllCaMsg;
	public static String ValidateUserJob_DialogTitle;
	public static String ValidateUserJob_Invalid6Ca;
	public static String ValidateUserJob_Invalid7Ca;
	public static String ValidateUserJob_smart6subtask;
	public static String ValidateUserJob_smart7subtask;
	public static String ValidateUserJob_taskname;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
