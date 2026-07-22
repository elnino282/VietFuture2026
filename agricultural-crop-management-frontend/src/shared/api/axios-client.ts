import Axios, { AxiosRequestConfig, AxiosResponse } from 'axios';
import httpClient from './http';

export const customInstance = <T>(
  config: AxiosRequestConfig | string,
  options?: any
): Promise<T> => {
  const finalConfig = typeof config === 'string' 
    ? { url: config, ...options } 
    : { ...config, ...options };

  return httpClient(finalConfig) as unknown as Promise<T>;
};
