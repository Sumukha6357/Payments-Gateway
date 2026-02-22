"use client";

import { useMemo } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/form-controls";
import { endpoints } from "@/lib/api/endpoints";
import { createIdempotencyKey } from "@/lib/utils/ids";

const schema = z.object({
  url: z.string().url(),
  secret: z.string().min(16)
});

type FormValues = z.infer<typeof schema>;

export function CreateWebhookModal() {
  const queryClient = useQueryClient();
  const generatedSecret = useMemo(() => createIdempotencyKey().replace(/-/g, ""), []);
  const { register, handleSubmit, setValue } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { secret: generatedSecret }
  });

  const mutation = useMutation({
    mutationFn: (values: FormValues) => endpoints.createWebhook(values),
    onSuccess: () => {
      toast.success("Webhook endpoint created");
      queryClient.invalidateQueries({ queryKey: ["webhooks"] });
    },
    onError: () => toast.error("Failed to create endpoint")
  });

  return (
    <Dialog title="Create Webhook Endpoint" trigger={<Button>Create Endpoint</Button>}>
      <form className="space-y-3" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
        <Input placeholder="Endpoint URL" {...register("url")} />
        <div className="flex gap-2">
          <Input placeholder="Secret" {...register("secret")} />
          <Button
            type="button"
            className="shrink-0"
            onClick={() => setValue("secret", createIdempotencyKey().replace(/-/g, ""))}
          >
            Regenerate
          </Button>
        </div>
        <Button className="w-full" type="submit">Save</Button>
      </form>
    </Dialog>
  );
}
