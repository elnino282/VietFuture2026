export default {
  ...[
    { name: 'identity', port: 8081 },
    { name: 'crop-catalog', port: 8082 },
    { name: 'ai', port: 8083 },
    { name: 'farm', port: 8084 },
    { name: 'season', port: 8085 },
    { name: 'inventory', port: 8086 },
    { name: 'finance', port: 8087 },
    { name: 'incident', port: 8088 },
    { name: 'sustainability', port: 8089 },
    { name: 'marketplace', port: 8090 },
    { name: 'admin-reporting', port: 8091 },
  ].reduce((acc, service) => {
    acc[`${service.name}Service`] = {
      input: {
        target: `http://localhost:${service.port}/v3/api-docs`,
      },
      output: {
        mode: 'split',
        target: `src/api/generated/${service.name}-service.ts`,
        schemas: `src/api/generated/model/${service.name}`,
        client: 'react-query',
        prettier: true,
        override: {
          mutator: {
            path: 'src/api/axios-client.ts',
            name: 'customInstance',
          },
        },
      },
    };
    return acc;
  }, {})
};
