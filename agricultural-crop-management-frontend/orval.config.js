export default {
  seasonService: {
    input: {
      target: '../docs/openapi/season-service-v1.yaml',
    },
    output: {
      mode: 'split',
      target: 'src/api/generated/season-service.ts',
      schemas: 'src/api/generated/model',
      client: 'react-query',
      prettier: true,
      override: {
        mutator: {
          path: 'src/api/axios-client.ts',
          name: 'customInstance',
        },
      },
    },
  },
};
