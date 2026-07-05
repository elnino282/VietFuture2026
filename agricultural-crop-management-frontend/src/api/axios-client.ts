import Axios, { AxiosRequestConfig, AxiosResponse } from 'axios';
import httpClient from '../shared/api/http';

export const customInstance = <T>(
  config: AxiosRequestConfig | string,
  options?: any
): Promise<T> => {
  const source = Axios.CancelToken.source();
  
  // Orval might pass (url, options) or (options)
  const finalConfig = typeof config === 'string' 
    ? { url: config, ...options } 
    : { ...config, ...options };

  const promise = httpClient({
    ...finalConfig,
    cancelToken: source.token,
  });

  // @ts-ignore
  promise.cancel = () => {
    source.cancel('Query was cancelled');
  };

  return promise as unknown as Promise<T>;
};
