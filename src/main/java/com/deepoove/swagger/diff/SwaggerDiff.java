package com.deepoove.swagger.diff;

import com.deepoove.swagger.diff.compare.MapKeyDiff;
import com.deepoove.swagger.diff.compare.ParameterDiff;
import com.deepoove.swagger.diff.compare.PropertyDiff;
import com.deepoove.swagger.diff.model.ChangedEndpoint;
import com.deepoove.swagger.diff.model.ChangedOperation;
import com.deepoove.swagger.diff.model.Endpoint;
import io.swagger.models.*;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;
import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.parser.SwaggerParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class SwaggerDiff {
      public static final String SWAGGER_VERSION_V2 = "2.0";

      private static Logger logger = LoggerFactory.getLogger(SwaggerDiff.class);

      private Swagger oldSpecSwagger;
      private Swagger newSpecSwagger;

      private List<Endpoint> newEndpoints;
      private List<Endpoint> missingEndpoints;
      private List<ChangedEndpoint> changedEndpoints;

      /**
       * Compares two v1.x Swagger spec files
       *
       * @param oldSpec Location (File or Http) of the spec file
       * @param newSpec Location (File or Http) of the spec file
       */
      public static SwaggerDiff compareV1(String oldSpec, String newSpec) {
            return compare(oldSpec, newSpec, null, null);
      }

      /**
       * Compares two v2.0 Swagger spec files
       *
       * @param oldSpec Location (File or Http) of the spec file
       * @param newSpec Location (File or Http) of the spec file
       */
      public static SwaggerDiff compareV2(String oldSpec, String newSpec) {
            return compare(oldSpec, newSpec, null, SWAGGER_VERSION_V2);
      }

      /**
       * Compares two swagger spec files of a given version with the given authorizations
       *
       * @param oldSpec Location (File or Http) of the spec file
       * @param newSpec Location (File or Http) of the spec file
       * @param auths   AuthorizationValues to be used parsing the spec file
       * @param version The version of the swagger spec files to be compared
       */
      public static SwaggerDiff compare(String oldSpec, String newSpec, List<AuthorizationValue> auths, String version) {
            return new SwaggerDiff(oldSpec, newSpec, auths, version).compare();
      }

      private SwaggerDiff(String oldSpec, String newSpec, List<AuthorizationValue> auths, String version) {
            if (SWAGGER_VERSION_V2.equals(version)) {
                  final SwaggerParser swaggerParser = new SwaggerParser();
                  oldSpecSwagger = swaggerParser.read(oldSpec, auths, true);
                  newSpecSwagger = swaggerParser.read(newSpec, auths, true);
            } else {
                  final SwaggerCompatConverter swaggerCompatConverter = new SwaggerCompatConverter();
                  try {
                        oldSpecSwagger = swaggerCompatConverter.read(oldSpec, auths);
                        newSpecSwagger = swaggerCompatConverter.read(newSpec, auths);
                  } catch (IOException e) {
                        logger.error("cannot read api-doc from spec[version_v1.x]", e);
                        return;
                  }
            }

            if (oldSpecSwagger == null || newSpecSwagger == null) {
                  throw new RuntimeException("cannot read api-doc from spec.");
            }
      }

      private SwaggerDiff compare() {
            final Map<String, Path> oldPaths = oldSpecSwagger.getPaths();
            final Map<String, Path> newPaths = newSpecSwagger.getPaths();
            final MapKeyDiff<String, Path> pathDiff = MapKeyDiff.diff(oldPaths, newPaths);

            this.newEndpoints = convert2EndpointList(pathDiff.getIncreased());
            this.missingEndpoints = convert2EndpointList(pathDiff.getMissing());
            this.changedEndpoints = new ArrayList<ChangedEndpoint>();

            final List<String> sharedKey = pathDiff.getSharedKey();
            ChangedEndpoint changedEndpoint = null;
            for (String pathUrl : sharedKey) {
                  changedEndpoint = new ChangedEndpoint();
                  changedEndpoint.setPathUrl(pathUrl);
                  Path oldPath = oldPaths.get(pathUrl);
                  Path newPath = newPaths.get(pathUrl);

                  final Map<HttpMethod, Operation> oldOperationMap = oldPath.getOperationMap();
                  final Map<HttpMethod, Operation> newOperationMap = newPath.getOperationMap();
                  final MapKeyDiff<HttpMethod, Operation> operationDiff = MapKeyDiff.diff(oldOperationMap, newOperationMap);
                  final Map<HttpMethod, Operation> increasedOperation = operationDiff.getIncreased();
                  final Map<HttpMethod, Operation> missingOperation = operationDiff.getMissing();
                  changedEndpoint.setNewOperations(increasedOperation);
                  changedEndpoint.setMissingOperations(missingOperation);

                  final List<HttpMethod> sharedMethods = operationDiff.getSharedKey();
                  final Map<HttpMethod, ChangedOperation> changedOperations = new HashMap<HttpMethod, ChangedOperation>();
                  ChangedOperation changedOperation = null;
                  for (HttpMethod method : sharedMethods) {
                        changedOperation = new ChangedOperation();
                        final Operation oldOperation = oldOperationMap.get(method);
                        final Operation newOperation = newOperationMap.get(method);
                        changedOperation.setSummary(newOperation.getSummary());

                        final List<Parameter> oldParameters = oldOperation.getParameters();
                        final List<Parameter> newParameters = newOperation.getParameters();
                        final ParameterDiff parameterDiff = ParameterDiff.buildWithDefinition(oldSpecSwagger.getDefinitions(), newSpecSwagger.getDefinitions()).diff(oldParameters, newParameters);
                        changedOperation.setAddParameters(parameterDiff.getIncreased());
                        changedOperation.setMissingParameters(parameterDiff.getMissing());
                        changedOperation.setChangedParameter(parameterDiff.getChanged());

                        final Property oldResponseProperty = getResponseProperty(oldOperation);
                        final Property newResponseProperty = getResponseProperty(newOperation);
                        final PropertyDiff propertyDiff = PropertyDiff.buildWithDefinition(oldSpecSwagger.getDefinitions(), newSpecSwagger.getDefinitions());
                        propertyDiff.diff(oldResponseProperty, newResponseProperty);
                        changedOperation.setAddProps(propertyDiff.getIncreased());
                        changedOperation.setMissingProps(propertyDiff.getMissing());

                        if (changedOperation.isDiff()) {
                              changedOperations.put(method, changedOperation);
                        }
                  }
                  changedEndpoint.setChangedOperations(changedOperations);

                  this.newEndpoints.addAll(convert2EndpointList(changedEndpoint.getPathUrl(), changedEndpoint.getNewOperations()));
                  this.missingEndpoints.addAll(convert2EndpointList(changedEndpoint.getPathUrl(), changedEndpoint.getMissingOperations()));

                  if (changedEndpoint.isDiff()) {
                        changedEndpoints.add(changedEndpoint);
                  }
            }

            return this;
      }

      private Property getResponseProperty(Operation operation) {
            final Map<String, Response> responses = operation.getResponses();
            final Response response = responses.get("200");

            return response == null ? null : response.getSchema();
      }

      private List<Endpoint> convert2EndpointList(Map<String, Path> map) {
            final List<Endpoint> endpoints = new ArrayList<Endpoint>();
            if (map != null) {
                  for (Entry<String, Path> entry : map.entrySet()) {
                        final String url = entry.getKey();
                        final Path path = entry.getValue();

                        final Map<HttpMethod, Operation> operationMap = path.getOperationMap();
                        for (Entry<HttpMethod, Operation> entryOper : operationMap.entrySet()) {
                              final HttpMethod httpMethod = entryOper.getKey();
                              final Operation operation = entryOper.getValue();

                              final Endpoint endpoint = new Endpoint();
                              endpoint.setPathUrl(url);
                              endpoint.setMethod(httpMethod);
                              endpoint.setSummary(operation.getSummary());
                              endpoint.setPath(path);
                              endpoint.setOperation(operation);
                              endpoints.add(endpoint);
                        }
                  }
            }

            return endpoints;
      }

      private Collection<? extends Endpoint> convert2EndpointList(String pathUrl, Map<HttpMethod, Operation> map) {
            final List<Endpoint> endpoints = new ArrayList<Endpoint>();
            if (map != null) {
                  for (Entry<HttpMethod, Operation> entry : map.entrySet()) {
                        final HttpMethod httpMethod = entry.getKey();
                        final Operation operation = entry.getValue();
                        final Endpoint endpoint = new Endpoint();
                        endpoint.setPathUrl(pathUrl);
                        endpoint.setMethod(httpMethod);
                        endpoint.setSummary(operation.getSummary());
                        endpoint.setOperation(operation);
                        endpoints.add(endpoint);
                  }
            }

            return endpoints;
      }

      public List<Endpoint> getNewEndpoints() {
            return newEndpoints;
      }

      public List<Endpoint> getMissingEndpoints() {
            return missingEndpoints;
      }

      public List<ChangedEndpoint> getChangedEndpoints() {
            return changedEndpoints;
      }
}
