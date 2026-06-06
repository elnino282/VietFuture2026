import { z } from "zod";

export interface EmployeeProfileData {
  id: number;
  username: string;
  displayName: string;
  email: string;
  phone: string;
  address: string;
  bio?: string;
  provinceId?: number;
  wardId?: number;
  role: "employee";
  status: "active" | "inactive";
  joinedDate: string;
  lastLogin: string;
  avatarUrl?: string;
}

export interface RecentActivity {
  id: string;
  type: "task" | "progress" | "payroll" | "system";
  date: string;
  description: string;
}

export interface NotificationPreferences {
  taskUpdates: boolean;
  payrollUpdates: boolean;
}

const PHONE_REGEX = /^[+]?[\d\s\-().]{7,20}$/;

export const EditProfileFormSchema = z.object({
  fullName: z
    .string()
    .trim()
    .min(1, "employee.profile.editDialog.validation.fullNameRequired")
    .max(255, "employee.profile.editDialog.validation.fullNameTooLong"),
  phone: z
    .string()
    .trim()
    .max(30, "employee.profile.editDialog.validation.phoneTooLong")
    .refine((value) => value === "" || PHONE_REGEX.test(value), {
      message: "employee.profile.editDialog.validation.invalidPhone",
    })
    .optional(),
  provinceId: z.number().optional(),
  wardId: z.number().optional(),
});

export type EditProfileFormData = z.infer<typeof EditProfileFormSchema>;
