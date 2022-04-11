/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *     Jesper Steen Moeller - Contribution for
 *                          Bug 406973 - [compiler] Parse MethodParameters attribute
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

import java.text.MessageFormat;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.jdt.internal.core.util.messages";//$NON-NLS-1$

	private Messages() {
		// Do not instantiate
	}

	public static String hierarchy_nullProject;
	public static String hierarchy_nullRegion;
	public static String hierarchy_nullFocusType;
	public static String hierarchy_creating;
	public static String hierarchy_creatingOnType;
	public static String element_doesNotExist;
	public static String element_notOnClasspath;
	public static String element_invalidClassFileName;
	public static String element_reconciling;
	public static String element_attachingSource;
	public static String element_invalidResourceForProject;
	public static String element_nullName;
	public static String element_nullType;
	public static String element_illegalParent;
	public static String element_moduleInfoNotSupported;
	public static String javamodel_initialization;
	public static String javamodel_initializing_delta_state;
	public static String javamodel_building_after_upgrade;
	public static String javamodel_configuring;
	public static String javamodel_configuring_classpath_containers;
	public static String javamodel_configuring_searchengine;
	public static String javamodel_getting_build_state_number;
	public static String javamodel_refreshing_external_jars;
	public static String javamodel_resetting_source_attachment_properties;
	public static String operation_needElements;
	public static String operation_needName;
	public static String operation_needPath;
	public static String operation_needAbsolutePath;
	public static String operation_needString;
	public static String operation_notSupported;
	public static String operation_cancelled;
	public static String operation_nullContainer;
	public static String operation_nullName;
	public static String operation_copyElementProgress;
	public static String operation_moveElementProgress;
	public static String operation_renameElementProgress;
	public static String operation_copyResourceProgress;
	public static String operation_moveResourceProgress;
	public static String operation_renameResourceProgress;
	public static String operation_createUnitProgress;
	public static String operation_createFieldProgress;
	public static String operation_createImportsProgress;
	public static String operation_createInitializerProgress;
	public static String operation_createMethodProgress;
	public static String operation_createPackageProgress;
	public static String operation_createPackageFragmentProgress;
	public static String operation_createTypeProgress;
	public static String operation_deleteElementProgress;
	public static String operation_deleteResourceProgress;
	public static String operation_cannotRenameDefaultPackage;
	public static String operation_pathOutsideProject;
	public static String operation_sortelements;
	public static String workingCopy_commit;
	public static String buffer_closed;
	public static String build_preparingBuild;
	public static String build_readStateProgress;
	public static String build_saveStateProgress;
	public static String build_saveStateComplete;
	public static String build_readingDelta;
	public static String build_analyzingDeltas;
	public static String build_analyzingSources;
	public static String build_cleaningOutput;
	public static String build_copyingResources;
	public static String build_compiling;
	public static String build_foundHeader;
	public static String build_fixedHeader;
	public static String build_oneError;
	public static String build_oneWarning;
	public static String build_multipleErrors;
	public static String build_multipleWarnings;
	public static String build_done;
	public static String build_wrongFileFormat;
	public static String build_cannotSaveState;
	public static String build_cannotSaveStates;
	public static String build_initializationError;
	public static String build_serializationError;
	public static String build_classFileCollision;
	public static String build_duplicateClassFile;
	public static String build_duplicateResource;
	public static String build_inconsistentClassFile;
	public static String build_inconsistentProject;
	public static String build_incompleteClassPath;
	public static String build_errorOnModuleDirective;
	public static String build_missingSourceFile;
	public static String build_prereqProjectHasClasspathProblems;
	public static String build_prereqProjectMustBeRebuilt;
	public static String build_abortDueToClasspathProblems;
	public static String status_cannot_retrieve_attached_javadoc;
	public static String status_timeout_javadoc;
	public static String status_cannotUseDeviceOnPath;
	public static String status_coreException;
	public static String status_defaultPackageReadOnly;
	public static String status_evaluationError;
	public static String status_JDOMError;
	public static String status_IOException;
	public static String status_indexOutOfBounds;
	public static String status_invalidContents;
	public static String status_invalidDestination;
	public static String status_invalidName;
	public static String status_invalidPackage;
	public static String status_invalidPath;
	public static String status_invalidProject;
	public static String status_invalidResource;
	public static String status_invalidResourceType;
	public static String status_invalidSibling;
	public static String status_nameCollision;
	public static String status_noLocalContents;
	public static String status_OK;
	public static String status_readOnly;
	public static String status_targetException;
	public static String status_unknown_javadoc_format;
	public static String status_updateConflict;
	public static String classpath_buildPath;
	public static String classpath_cannotNestEntryInEntry;
	public static String classpath_cannotNestEntryInEntryNoExclusion;
	public static String classpath_cannotNestEntryInLibrary;
	public static String classpath_cannotNestEntryInOutput;
	public static String classpath_cannotNestOutputInEntry;
	public static String classpath_cannotNestOutputInOutput;
	public static String classpath_cannotReadClasspathFile;
	public static String classpath_cannotReferToItself;
	public static String classpath_cannotUseDistinctSourceFolderAsOutput;
	public static String classpath_cannotUseLibraryAsOutput;
	public static String classpath_closedProject;
	public static String classpath_couldNotWriteClasspathFile;
	public static String classpath_cycle;
	public static String classpath_duplicateEntryPath;
	public static String classpath_illegalContainerPath;
	public static String classpath_illegalEntryInClasspathFile;
	public static String classpath_illegalLibraryPath;
	public static String classpath_illegalLibraryPathInContainer;
	public static String classpath_illegalLibraryArchive;
	public static String classpath_archiveReadError;
	public static String classpath_illegalExternalFolder;
	public static String classpath_illegalExternalFolderInContainer;
	public static String classpath_illegalProjectPath;
	public static String classpath_illegalSourceFolderPath;
	public static String classpath_illegalVariablePath;
	public static String classpath_invalidClasspathInClasspathFile;
	public static String classpath_invalidContainer;
	public static String classpath_mustEndWithSlash;
	public static String classpath_unboundContainerPath;
	public static String classpath_unboundLibrary;
	public static String classpath_userLibraryInfo;
	public static String classpath_containerInfo;
	public static String classpath_unboundLibraryInContainer;
	public static String classpath_unboundProject;
	public static String classpath_settingOutputLocationProgress;
	public static String classpath_settingProgress;
	public static String classpath_unboundSourceAttachment;
	public static String classpath_unboundSourceAttachmentInContainedLibrary;
	public static String classpath_unboundSourceFolder;
	public static String classpath_unboundVariablePath;
	public static String classpath_unknownKind;
	public static String classpath_xmlFormatError;
	public static String classpath_disabledInclusionExclusionPatterns;
	public static String classpath_disabledMultipleOutputLocations;
	public static String classpath_incompatibleLibraryJDKLevel;
	public static String classpath_incompatibleLibraryJDKLevelInContainer;
	public static String classpath_duplicateEntryExtraAttribute;
	public static String classpath_deprecated_variable;
	public static String classpath_invalidExternalAnnotationPath;
	public static String classpath_testSourceRequiresSeparateOutputFolder;
	public static String classpath_testOutputFolderMustBeSeparateFromMainOutputFolders;
	public static String classpath_main_only_project_depends_on_test_only_project;
	public static String file_notFound;
	public static String file_badFormat;
	public static String path_nullPath;
	public static String path_mustBeAbsolute;
	public static String cache_invalidLoadFactor;
	public static String savedState_jobName;
	public static String refreshing_external_folders;
	public static String synchronizing_projects_job;
	public static String convention_unit_nullName;
	public static String convention_unit_notJavaName;
	public static String convention_classFile_nullName;
	public static String convention_classFile_notClassFileName;
	public static String convention_illegalIdentifier;
	public static String convention_import_nullImport;
	public static String convention_import_unqualifiedImport;
	public static String convention_type_nullName;
	public static String convention_type_nameWithBlanks;
	public static String convention_type_dollarName;
	public static String convention_type_lowercaseName;
	public static String convention_type_invalidName;
	public static String convention_package_nullName;
	public static String convention_package_emptyName;
	public static String convention_package_dotName;
	public static String convention_package_nameWithBlanks;
	public static String convention_package_consecutiveDotsName;
	public static String convention_package_uppercaseName;
	public static String convention_module_nullName;
	public static String convention_module_emptyName;
	public static String convention_module_dotName;
	public static String convention_module_nameWithBlanks;
	public static String convention_module_consecutiveDotsName;
	public static String convention_module_uppercaseName;
	public static String convention_module_javaName;
	public static String dom_cannotDetail;
	public static String dom_nullTypeParameter;
	public static String dom_nullNameParameter;
	public static String dom_nullReturnType;
	public static String dom_nullExceptionType;
	public static String dom_mismatchArgNamesAndTypes;
	public static String dom_addNullChild;
	public static String dom_addIncompatibleChild;
	public static String dom_addChildWithParent;
	public static String dom_unableAddChild;
	public static String dom_addAncestorAsChild;
	public static String dom_addNullSibling;
	public static String dom_addSiblingBeforeRoot;
	public static String dom_addIncompatibleSibling;
	public static String dom_addSiblingWithParent;
	public static String dom_addAncestorAsSibling;
	public static String dom_addNullInterface;
	public static String dom_nullInterfaces;
	public static String importRewrite_processDescription;
	public static String correction_nullRequestor;
	public static String correction_nullUnit;
	public static String engine_completing;
	public static String engine_searching;
	public static String engine_searching_indexing;
	public static String engine_searching_matching;
	public static String exception_wrongFormat;
	public static String process_name;
	public static String jobmanager_filesToIndex;
	public static String jobmanager_indexing;
	public static String disassembler_description;
	public static String disassembler_opentypedeclaration;
	public static String disassembler_closetypedeclaration;
	public static String disassembler_parametername;
	public static String disassembler_anonymousparametername;
	public static String disassembler_localvariablename;
	public static String disassembler_endofmethodheader;
	public static String disassembler_begincommentline;
	public static String disassembler_fieldhasconstant;
	public static String disassembler_endoffieldheader;
	public static String disassembler_sourceattributeheader;
	public static String disassembler_enclosingmethodheader;
	public static String disassembler_exceptiontableheader;
	public static String disassembler_linenumberattributeheader;
	public static String disassembler_methodparametersheader;
	public static String disassembler_localvariabletableattributeheader;
	public static String disassembler_localvariabletypetableattributeheader;
	public static String disassembler_arraydimensions;
	public static String disassembler_innerattributesheader;
	public static String disassembler_inner_class_info_name;
	public static String disassembler_outer_class_info_name;
	public static String disassembler_inner_name;
	public static String disassembler_inner_accessflags;
	public static String disassembler_nesthost;
	public static String disassembler_nestmembers;
	public static String disassembler_record;
	public static String disassembler_permittedsubclasses;
	public static String disassembler_components;
	public static String disassembler_endofcomponent;
	public static String disassembler_genericattributeheader;
	public static String disassembler_signatureattributeheader;
	public static String disassembler_bootstrapmethodattributesheader;
	public static String disassembler_bootstrapmethodentry;
	public static String disassembler_bootstrapmethodentry_argument;
	public static String disassembler_indentation;
	public static String disassembler_constantpoolindex;
	public static String disassembler_space;
	public static String disassembler_comma;
	public static String disassembler_openinnerclassentry;
	public static String disassembler_closeinnerclassentry;
	public static String disassembler_deprecated;
	public static String disassembler_constantpoolheader;
	public static String disassembler_constantpool_class;
	public static String disassembler_constantpool_double;
	public static String disassembler_constantpool_float;
	public static String disassembler_constantpool_integer;
	public static String disassembler_constantpool_long;
	public static String disassembler_constantpool_string;
	public static String disassembler_constantpool_fieldref;
	public static String disassembler_constantpool_interfacemethodref;
	public static String disassembler_constantpool_methodref;
	public static String disassembler_constantpool_name_and_type;
	public static String disassembler_constantpool_utf8;
	public static String disassembler_constantpool_methodhandle;
	public static String disassembler_constantpool_methodtype;
	public static String disassembler_constantpool_invokedynamic;
	public static String disassembler_constantpool_dynamic;
	public static String disassembler_annotationdefaultheader;
	public static String disassembler_annotationdefaultvalue;
	public static String disassembler_annotationenumvalue;
	public static String disassembler_annotationclassvalue;
	public static String disassembler_annotationannotationvalue;
	public static String disassembler_annotationarrayvaluestart;
	public static String disassembler_annotationarrayvalueend;
	public static String disassembler_annotationentrystart;
	public static String disassembler_annotationentryend;
	public static String disassembler_annotationcomponent;
	// jsr308
	public static String disassembler_extendedannotationentrystart;
	public static String disassembler_extendedannotationentryend;
	public static String disassembler_runtimevisibletypeannotationsattributeheader;
	public static String disassembler_runtimeinvisibletypeannotationsattributeheader;
	public static String disassembler_extendedannotation_classextendsimplements;
	public static String disassembler_extendedannotation_typepath;
	public static String disassembler_extendedannotation_method_parameter;
	public static String disassembler_extendedannotation_offset;
	public static String disassembler_extendedannotation_throws;
	public static String disassembler_extendedannotation_type_argument;
	public static String disassembler_extendedannotation_type_parameter;
	public static String disassembler_extendedannotation_type_parameter_with_bound;
	public static String disassembler_extendedannotation_wildcardlocationtype;
	public static String disassembler_extendedannotation_targetType;
	public static String disassembler_extendedannotation_wildcardlocations;
	public static String disassembler_extendedannotation_exception_table_index;
	public static String disassembler_extendedannotation_typepath_array;
	public static String disassembler_extendedannotation_typepath_wildcard;
	public static String disassembler_extendedannotation_typepath_typeargument;
	public static String disassembler_extendedannotation_typepath_innertype;
	public static String disassembler_localvariabletargetheader;
	public static String disassembler_module_version;
	public static String disassembler_module_version_none;
	public static String disassembler_modulepackagesattributeheader;
	public static String disassembler_modulemainclassattributeheader;

	public static String disassembler_runtimevisibleannotationsattributeheader;
	public static String disassembler_runtimeinvisibleannotationsattributeheader;
	public static String disassembler_runtimevisibleparameterannotationsattributeheader;
	public static String disassembler_runtimeinvisibleparameterannotationsattributeheader;
	public static String disassembler_parameterannotationentrystart;
	public static String disassembler_stackmaptableattributeheader;
	public static String disassembler_stackmapattributeheader;
	public static String classfileformat_versiondetails;
	public static String classfileformat_methoddescriptor;
	public static String classfileformat_fieldddescriptor;
	public static String classfileformat_stacksAndLocals;
	public static String classfileformat_superflagisnotset;
	public static String classfileformat_superflagisset;
	public static String classfileformat_clinitname;
	// jsr308
	public static String classfileformat_localvariablereferenceinfoentry;

	public static String classformat_classformatexception;
	public static String classformat_anewarray;
	public static String classformat_checkcast;
	public static String classformat_instanceof;
	public static String classformat_ldc_w_class;
	public static String classformat_ldc_w_methodtype;
	public static String classformat_ldc_w_methodhandle;
	public static String classformat_ldc_w_dynamic;
	public static String classformat_ldc_w_float;
	public static String classformat_ldc_w_integer;
	public static String classformat_ldc_w_string;
	public static String classformat_ldc2_w_long;
	public static String classformat_ldc2_w_double;
	public static String classformat_multianewarray;
	public static String classformat_new;
	public static String classformat_iinc;
	public static String classformat_invokespecial;
	public static String classformat_invokeinterface;
	public static String classformat_invokestatic;
	public static String classformat_invokevirtual;
	public static String classformat_invokedynamic;
	public static String classformat_getfield;
	public static String classformat_getstatic;
	public static String classformat_putstatic;
	public static String classformat_putfield;
	public static String classformat_newarray_boolean;
	public static String classformat_newarray_char;
	public static String classformat_newarray_float;
	public static String classformat_newarray_double;
	public static String classformat_newarray_byte;
	public static String classformat_newarray_short;
	public static String classformat_newarray_int;
	public static String classformat_newarray_long;
	public static String classformat_store;
	public static String classformat_load;
	public static String classfileformat_anyexceptionhandler;
	public static String classfileformat_exceptiontableentry;
	public static String classfileformat_linenumbertableentry;
	public static String classfileformat_localvariabletableentry;
	public static String classfileformat_versionUnknown;
	public static String classfileformat_componentdescriptor;

	public static String disassembler_frame_same_locals_1_stack_item_extended;
	public static String disassembler_frame_chop;
	public static String disassembler_frame_same_frame_extended;
	public static String disassembler_frame_append;
	public static String disassembler_frame_full_frame;
	public static String disassembler_frame_same_frame;
	public static String disassembler_frame_same_locals_1_stack_item;
	public static String internal_error;

	public static String disassembler_method_type_ref_getfield;
	public static String disassembler_method_type_ref_putfield;
	public static String disassembler_method_type_ref_getstatic;
	public static String disassembler_method_type_ref_putstatic;
	public static String disassembler_method_type_ref_invokestatic;
	public static String disassembler_method_type_ref_invokevirtual;
	public static String disassembler_method_type_ref_invokespecial;
	public static String disassembler_method_type_ref_invokeinterface;
	public static String disassembler_method_type_ref_newinvokespecial;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	/**
	 * Bind the given message's substitution locations with the given string values.
	 *
	 * @param message the message to be manipulated
	 * @return the manipulated String
	 */
	public static String bind(String message) {
		return bind(message, null);
	}

	/**
	 * Bind the given message's substitution locations with the given string values.
	 *
	 * @param message the message to be manipulated
	 * @param binding the object to be inserted into the message
	 * @return the manipulated String
	 */
	public static String bind(String message, Object binding) {
		return bind(message, new Object[] {binding});
	}

	/**
	 * Bind the given message's substitution locations with the given string values.
	 *
	 * @param message the message to be manipulated
	 * @param binding1 An object to be inserted into the message
	 * @param binding2 A second object to be inserted into the message
	 * @return the manipulated String
	 */
	public static String bind(String message, Object binding1, Object binding2) {
		return bind(message, new Object[] {binding1, binding2});
	}

	/**
	 * Bind the given message's substitution locations with the given string values.
	 *
	 * @param message the message to be manipulated
	 * @param bindings An array of objects to be inserted into the message
	 * @return the manipulated String
	 */
	public static String bind(String message, Object[] bindings) {
		return MessageFormat.format(message, bindings);
	}
}
