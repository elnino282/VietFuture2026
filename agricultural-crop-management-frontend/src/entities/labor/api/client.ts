import { TaskSchema } from "@/entities/task";
import httpClient from "@/shared/api/http";
import { parseApiResponse, parsePageResponse, type PageResponse } from "@/shared/api/types";
import { z } from "zod";
import {
  AddSeasonEmployeeRequestSchema,
  AssignTaskEmployeeRequestSchema,
  BulkAssignSeasonEmployeesRequestSchema,
  EmployeeTaskProgressRequestSchema,
  EmployeeDirectorySchema,
  PayrollRecordSchema,
  PayrollRecalculateRequestSchema,
  PayrollRecordUpdateRequestSchema,
  SeasonEmployeeSchema,
  TaskProgressLogSchema,
  UpdateSeasonEmployeeRequestSchema,
} from "../model/schemas";
import type {
  AddSeasonEmployeeRequest,
  AssignTaskEmployeeRequest,
  BulkAssignSeasonEmployeesRequest,
  EmployeeTaskProgressRequest,
  EmployeeDirectory,
  PayrollRecord,
  PayrollRecalculateRequest,
  PayrollRecordUpdateRequest,
  SeasonEmployee,
  TaskProgressLog,
  UpdateSeasonEmployeeRequest,
} from "../model/types";
import type { Task } from "@/entities/task";

export const laborApi = {
  listEmployeeDirectory: async (params?: {
    keyword?: string;
    page?: number;
    size?: number;
  }): Promise<PageResponse<EmployeeDirectory>> => {
    const response = await httpClient.get("/api/v1/farmer/labor/employees/directory", { params });
    return parsePageResponse(response.data, EmployeeDirectorySchema);
  },

  listSeasonEmployees: async (
    seasonId: number,
    params?: { keyword?: string; page?: number; size?: number }
  ): Promise<PageResponse<SeasonEmployee>> => {
    const response = await httpClient.get(`/api/v1/farmer/labor/seasons/${seasonId}/employees`, { params });
    return parsePageResponse(response.data, SeasonEmployeeSchema);
  },

  addSeasonEmployee: async (seasonId: number, data: AddSeasonEmployeeRequest): Promise<SeasonEmployee> => {
    const payload = AddSeasonEmployeeRequestSchema.parse(data);
    const response = await httpClient.post(`/api/v1/farmer/labor/seasons/${seasonId}/employees`, payload);
    return parseApiResponse(response.data, SeasonEmployeeSchema);
  },

  bulkAssignSeasonEmployees: async (
    seasonId: number,
    data: BulkAssignSeasonEmployeesRequest
  ): Promise<SeasonEmployee[]> => {
    const payload = BulkAssignSeasonEmployeesRequestSchema.parse(data);
    const response = await httpClient.post(
      `/api/v1/farmer/labor/seasons/${seasonId}/employees/bulk`,
      payload
    );
    return parseApiResponse(response.data, z.array(SeasonEmployeeSchema));
  },

  updateSeasonEmployee: async (
    seasonId: number,
    employeeUserId: number,
    data: UpdateSeasonEmployeeRequest
  ): Promise<SeasonEmployee> => {
    const payload = UpdateSeasonEmployeeRequestSchema.parse(data);
    const response = await httpClient.patch(
      `/api/v1/farmer/labor/seasons/${seasonId}/employees/${employeeUserId}`,
      payload
    );
    return parseApiResponse(response.data, SeasonEmployeeSchema);
  },

  removeSeasonEmployee: async (seasonId: number, employeeUserId: number): Promise<void> => {
    await httpClient.delete(`/api/v1/farmer/labor/seasons/${seasonId}/employees/${employeeUserId}`);
  },

  assignTaskToEmployee: async (taskId: number, data: AssignTaskEmployeeRequest): Promise<Task> => {
    const payload = AssignTaskEmployeeRequestSchema.parse(data);
    const response = await httpClient.patch(`/api/v1/farmer/labor/tasks/${taskId}/assign`, payload);
    return parseApiResponse(response.data, TaskSchema);
  },

  approveTask: async (taskId: number): Promise<Task> => {
    const response = await httpClient.patch(`/api/v1/farmer/labor/tasks/${taskId}/approve`);
    return parseApiResponse(response.data, TaskSchema);
  },

  rejectTask: async (taskId: number, data: { rejectReason: string }): Promise<Task> => {
    const response = await httpClient.post(`/api/v1/farmer/labor/tasks/${taskId}/reject`, data);
    return parseApiResponse(response.data, TaskSchema);
  },

  getTaskProgressLogs: async (taskId: number): Promise<TaskProgressLog[]> => {
    const response = await httpClient.get(`/api/v1/farmer/labor/tasks/${taskId}/progress-logs`);
    return parseApiResponse(response.data, z.array(TaskProgressLogSchema));
  },

  listSeasonProgress: async (
    seasonId: number,
    params?: { employeeUserId?: number; taskId?: number; page?: number; size?: number }
  ): Promise<PageResponse<TaskProgressLog>> => {
    const response = await httpClient.get(`/api/v1/farmer/labor/seasons/${seasonId}/progress`, { params });
    return parsePageResponse(response.data, TaskProgressLogSchema);
  },

  listSeasonPayroll: async (
    seasonId: number,
    params?: { employeeUserId?: number; page?: number; size?: number }
  ): Promise<PageResponse<PayrollRecord>> => {
    const response = await httpClient.get(`/api/v1/farmer/labor/seasons/${seasonId}/payroll`, { params });
    return parsePageResponse(response.data, PayrollRecordSchema);
  },

  getSeasonPayrollDetail: async (seasonId: number, payrollRecordId: number): Promise<PayrollRecord> => {
    const response = await httpClient.get(`/api/v1/farmer/labor/seasons/${seasonId}/payroll/${payrollRecordId}`);
    return parseApiResponse(response.data, PayrollRecordSchema);
  },

  updateSeasonPayroll: async (
    seasonId: number,
    payrollRecordId: number,
    data: PayrollRecordUpdateRequest
  ): Promise<PayrollRecord> => {
    const payload = PayrollRecordUpdateRequestSchema.parse(data);
    const response = await httpClient.patch(
      `/api/v1/farmer/labor/seasons/${seasonId}/payroll/${payrollRecordId}`,
      payload
    );
    return parseApiResponse(response.data, PayrollRecordSchema);
  },

  recalculateSeasonPayroll: async (
    seasonId: number,
    data?: PayrollRecalculateRequest
  ): Promise<PayrollRecord[]> => {
    const payload = data ? PayrollRecalculateRequestSchema.parse(data) : {};
    const response = await httpClient.post(`/api/v1/farmer/labor/seasons/${seasonId}/payroll/recalculate`, payload);
    return parseApiResponse(response.data, z.array(PayrollRecordSchema));
  },

  listMyTasks: async (params?: {
    status?: string;
    seasonId?: number;
    page?: number;
    size?: number;
  }): Promise<PageResponse<Task>> => {
    const response = await httpClient.get("/api/v1/employee/tasks", { params });
    return parsePageResponse(response.data, TaskSchema);
  },

  acceptTask: async (taskId: number): Promise<Task> => {
    const response = await httpClient.patch(`/api/v1/employee/tasks/${taskId}/accept`);
    return parseApiResponse(response.data, TaskSchema);
  },

  reportTaskProgress: async (taskId: number, data: EmployeeTaskProgressRequest): Promise<TaskProgressLog> => {
    const payload = EmployeeTaskProgressRequestSchema.parse(data);
    const response = await httpClient.post(`/api/v1/employee/tasks/${taskId}/progress`, payload);
    return parseApiResponse(response.data, TaskProgressLogSchema);
  },

  listMyProgress: async (params?: { page?: number; size?: number }): Promise<PageResponse<TaskProgressLog>> => {
    const response = await httpClient.get("/api/v1/employee/progress", { params });
    return parsePageResponse(response.data, TaskProgressLogSchema);
  },

  getMySeasonPlan: async (seasonId: number): Promise<Task[]> => {
    const response = await httpClient.get(`/api/v1/employee/seasons/${seasonId}/plan`);
    return parseApiResponse(response.data, z.array(TaskSchema));
  },

  listMyPayroll: async (params?: { seasonId?: number; page?: number; size?: number }): Promise<PageResponse<PayrollRecord>> => {
    const response = await httpClient.get("/api/v1/employee/payroll", { params });
    return parsePageResponse(response.data, PayrollRecordSchema);
  },

  getMyPayrollDetail: async (payrollRecordId: number): Promise<PayrollRecord> => {
    const response = await httpClient.get(`/api/v1/employee/payroll/${payrollRecordId}`);
    return parseApiResponse(response.data, PayrollRecordSchema);
  },
};
